FROM python:3.11-slim

WORKDIR /etl

# Copia os requisitos
COPY requirements.txt .

# Instala os pacotes
RUN pip install --no-cache-dir -r requirements.txt

# Copia o script principal
COPY import_sinapi.py .

# Copia a pasta de dados onde o usuário colocará os zips ou xlsx
# Usaremos um volume mapeado aqui na hora de rodar
RUN mkdir -p /etl/data

CMD ["python", "import_sinapi.py"]
