import pandas as pd
import json
import os

file_path = 'materiais-sienge-planilha-de-levantamento-quantitativos.xlsx'
out_dir = 'scripts/data/json_extracts'
os.makedirs(out_dir, exist_ok=True)

xl = pd.ExcelFile(file_path)

for sheet_name in xl.sheet_names:
    try:
        # Pula as primeiras linhas de cabeçalho que são layout de impressão
        df = pd.read_excel(file_path, sheet_name=sheet_name, skiprows=4)
        
        # Limpa colunas e linhas completamente vazias
        df.dropna(how='all', axis=1, inplace=True)
        df.dropna(how='all', axis=0, inplace=True)
        
        # Preenche os NaNs com string vazia
        df = df.fillna('')
        
        # Converte para dict
        data = df.to_dict(orient='records')
        
        # Salva o arquivo JSON com o nome da aba
        safe_name = sheet_name.replace(' ', '_').replace('/', '_')
        out_file = os.path.join(out_dir, f"{safe_name}.json")
        
        with open(out_file, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            
        print(f"[OK] Extraído: {sheet_name} ({len(data)} registros)")
    except Exception as e:
        print(f"[ERRO] Erro ao extrair {sheet_name}: {e}")

print(f"\nExtração concluída! Os arquivos estão na pasta: {out_dir}")
