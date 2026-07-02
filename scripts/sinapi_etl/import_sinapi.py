import os
import uuid
import pandas as pd
from sqlalchemy import create_engine, text
from datetime import datetime

# --- CONFIGURAÇÕES ---
# Em produção, essas variáveis viriam de variáveis de ambiente
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = os.getenv("DB_PORT", "5432")
DB_NAME = os.getenv("DB_NAME", "app_orc_certeiro")
DB_USER = os.getenv("DB_USER", "postgres")
DB_PASS = os.getenv("DB_PASS", "postgres")

# Caminhos dos arquivos (Exemplo: colocar os arquivos na pasta data/)
# O SINAPI libera dois arquivos: um com as Composições (serviços) e outro com Insumos (ingredientes)
# Para este MVP, vamos criar uma lógica genérica que lê CSVs ou Excel.
DATA_DIR = os.path.join(os.path.dirname(__file__), "data")
COMPOSICOES_FILE = os.path.join(DATA_DIR, "composicoes.csv")
INSUMOS_FILE = os.path.join(DATA_DIR, "insumos.csv")

# URL do banco de dados para o SQLAlchemy
DATABASE_URI = f"postgresql+psycopg2://{DB_USER}:{DB_PASS}@{DB_HOST}:{DB_PORT}/{DB_NAME}"

def create_db_engine():
    print(f"Conectando ao banco de dados: {DB_HOST}:{DB_PORT}/{DB_NAME}")
    return create_engine(DATABASE_URI)

def run_etl():
    engine = create_db_engine()

    # 1. Criação do Catálogo Base
    # Aqui simulamos a criação do Header da base SINAPI
    catalogo_id = str(uuid.uuid4())
    nome_catalogo = "SINAPI_IMPORTADO"
    mes_ano = datetime.now().strftime("%Y-%m")
    estado = "SP"
    is_desonerado = True

    print(f"1. Criando Catálogo Base: {nome_catalogo} - {mes_ano} - {estado}")
    with engine.begin() as conn:
        conn.execute(
            text("""
                INSERT INTO catalogos_bases (id, nome, mes_ano, estado, is_desonerado)
                VALUES (:id, :nome, :mes_ano, :estado, :is_desonerado)
            """),
            {
                "id": catalogo_id,
                "nome": nome_catalogo,
                "mes_ano": mes_ano,
                "estado": estado,
                "is_desonerado": is_desonerado
            }
        )

    # 2. Leitura e Limpeza dos Dados (Pandas)
    print("2. Lendo arquivos de dados...")
    
    if not os.path.exists(COMPOSICOES_FILE) or not os.path.exists(INSUMOS_FILE):
        print(f"AVISO: Arquivos não encontrados em {DATA_DIR}.")
        print("Criando arquivos de exemplo (mock) para demonstrar o ETL...")
        criar_arquivos_mock()

    # Lendo CSVs (O SINAPI costuma usar ISO-8859-1 e separador ponto-e-vírgula)
    try:
        df_composicoes = pd.read_csv(COMPOSICOES_FILE, sep=';', encoding='utf-8')
        df_insumos = pd.read_csv(INSUMOS_FILE, sep=';', encoding='utf-8')
    except Exception as e:
        print(f"Erro ao ler CSVs: {e}")
        return

    # 3. Transform e Load: Itens (Composições)
    print(f"3. Inserindo {len(df_composicoes)} Composições (Itens)...")
    
    # Criamos um dicionário para mapear codigo -> uuid gerado, pois os insumos precisarão do UUID do pai
    item_uuid_map = {}
    
    with engine.begin() as conn:
        for index, row in df_composicoes.iterrows():
            item_id = str(uuid.uuid4())
            item_uuid_map[str(row['CODIGO'])] = item_id
            
            conn.execute(
                text("""
                    INSERT INTO catalogos_itens 
                    (id, catalogo_id, codigo, descricao, unidade, valor_mo, valor_mat, valor_srv)
                    VALUES (:id, :catalogo_id, :codigo, :descricao, :unidade, :valor_mo, :valor_mat, :valor_srv)
                """),
                {
                    "id": item_id,
                    "catalogo_id": catalogo_id,
                    "codigo": str(row['CODIGO']),
                    "descricao": str(row['DESCRICAO']),
                    "unidade": str(row['UNIDADE']),
                    "valor_mo": float(row.get('VALOR_MO', 0)),
                    "valor_mat": float(row.get('VALOR_MAT', 0)),
                    "valor_srv": float(row.get('VALOR_SRV', 0))
                }
            )

    # 4. Transform e Load: Insumos (CPUs)
    print(f"4. Inserindo {len(df_insumos)} Insumos (CPUs)...")
    
    with engine.begin() as conn:
        for index, row in df_insumos.iterrows():
            codigo_composicao = str(row['CODIGO_COMPOSICAO'])
            
            # Pula insumos que não tem a composição pai na lista que inserimos
            if codigo_composicao not in item_uuid_map:
                continue
                
            item_id = item_uuid_map[codigo_composicao]
            insumo_id = str(uuid.uuid4())
            
            conn.execute(
                text("""
                    INSERT INTO catalogos_insumos 
                    (id, item_id, tipo_insumo, codigo, descricao, unidade, coeficiente, custo_unitario)
                    VALUES (:id, :item_id, :tipo_insumo, :codigo, :descricao, :unidade, :coeficiente, :custo_unitario)
                """),
                {
                    "id": insumo_id,
                    "item_id": item_id,
                    "tipo_insumo": str(row['TIPO_INSUMO']),
                    "codigo": str(row.get('CODIGO_INSUMO', '')),
                    "descricao": str(row['DESCRICAO']),
                    "unidade": str(row['UNIDADE']),
                    "coeficiente": float(row['COEFICIENTE']),
                    "custo_unitario": float(row['CUSTO_UNITARIO'])
                }
            )

    print("ETL concluído com sucesso!")


def criar_arquivos_mock():
    os.makedirs(DATA_DIR, exist_ok=True)
    
    # Mock Composições
    csv_comp = [
        "CODIGO;DESCRICAO;UNIDADE;VALOR_MO;VALOR_MAT;VALOR_SRV",
        "10001;PINTURA LATEX ACRILICA 2 DEMÃOS;m2;5.50;12.30;0.0",
        "10002;FORRO DE GESSO ACARTONADO;m2;15.00;35.00;0.0"
    ]
    with open(COMPOSICOES_FILE, "w", encoding="utf-8") as f:
        f.write("\n".join(csv_comp))
        
    # Mock Insumos
    csv_ins = [
        "CODIGO_COMPOSICAO;TIPO_INSUMO;CODIGO_INSUMO;DESCRICAO;UNIDADE;COEFICIENTE;CUSTO_UNITARIO",
        "10001;MAO_DE_OBRA;P100;PINTOR;H;0.25;22.00",
        "10001;MATERIAL;M200;TINTA LATEX ACRILICA;L;0.35;35.14",
        "10002;MAO_DE_OBRA;P101;MONTADOR DE GESSO;H;0.50;30.00",
        "10002;MATERIAL;M201;PLACA DE GESSO ACARTONADO;m2;1.05;33.33"
    ]
    with open(INSUMOS_FILE, "w", encoding="utf-8") as f:
        f.write("\n".join(csv_ins))


if __name__ == "__main__":
    run_etl()
