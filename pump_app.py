from fastapi import FastAPI, HTTPException, Header
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from typing import List
import io
import zipfile
import uuid

app = FastAPI(title="Mock ПУМП")

# Простое «хранилище» файлов в памяти: file_id -> bytes
FILES: dict[str, bytes] = {}

# Токен-заглушка
VALID_TOKEN = "test-token-123"


def check_token(authorization: str | None):
    if authorization != f"Bearer {VALID_TOKEN}":
        raise HTTPException(status_code=401, detail="Invalid token")


class PeriodInfo(BaseModel):
    period_id: str
    summ: float
    count: int
    file_ids: List[str]


@app.post("/auth/token")
def get_token():
    """Эмуляция Keycloak: выдаём статический токен."""
    return {"access_token": VALID_TOKEN, "token_type": "Bearer"}


@app.get("/pump/period/{period_id}", response_model=PeriodInfo)
def get_period_info(period_id: str, authorization: str | None = Header(default=None)):
    """
    Эмуляция метода ПУМП getPeriodInfo.
    Возвращает метаданные + список fileId.
    """
    check_token(authorization)

    # Генерируем 2 тестовых ZIP-файла и кладём в «хранилище»
    file_ids = []
    for i in range(2):
        file_id = f"{period_id}-file-{uuid.uuid4().hex[:8]}"
        buf = io.BytesIO()
        with zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED) as zf:
            zf.writestr(f"u_file_{i}.txt", f"Тестовый U-файл {i} за период {period_id}\n")
        FILES[file_id] = buf.getvalue()
        file_ids.append(file_id)

    return PeriodInfo(
        period_id=period_id,
        summ=12345.67,
        count=len(file_ids),
        file_ids=file_ids,
    )


@app.get("/filestore/download/{file_id}")
def download_file(file_id: str, authorization: str | None = Header(default=None)):
    """Эмуляция File Store: отдаём ZIP-файл по ID."""
    check_token(authorization)
    if file_id not in FILES:
        raise HTTPException(status_code=404, detail="File not found")

    return StreamingResponse(
        io.BytesIO(FILES[file_id]),
        media_type="application/zip",
        headers={"Content-Disposition": f"attachment; filename={file_id}.zip"},
    )