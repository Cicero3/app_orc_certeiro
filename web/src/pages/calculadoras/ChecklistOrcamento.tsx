import React, { useEffect, useMemo, useState } from 'react';
import { ClipboardCheck, RotateCcw } from 'lucide-react';
import { PageHeader, fmt, panelStyle, resumoPanelStyle } from './shared';

// Checklist detalhado de orçamento de obras (Engplay), transformado em
// módulo interativo com progresso salvo no navegador.

interface Secao {
  titulo: string;
  dica: string;
  itens: string[];
}

const SECOES: Secao[] = [
  {
    titulo: '1. Análise Inicial',
    dica: 'Reunião detalhada com o cliente: toda informação é necessária para o orçamento.',
    itens: [
      'Reunião inicial com o cliente',
      'Revisão dos documentos recebidos',
      'Estudo e compatibilização dos projetos (arquitetônico + complementares)',
      'Identificação dos requisitos técnicos',
      'Alinhamento de expectativas com o cliente',
    ],
  },
  {
    titulo: '2. Avaliação do Local',
    dica: 'Visite o local da obra para avaliar pessoalmente terreno, acesso e desafios.',
    itens: [
      'Topografia do terreno',
      'Condições e estudo do solo',
      'Acesso e logística',
      'Infraestrutura existente',
      'Restrições ambientais',
      'Vizinhança e entorno',
      'Riscos naturais',
      'Normas do condomínio',
    ],
  },
  {
    titulo: '3. Estimativa Preliminar de Custos',
    dica: 'Dados históricos + referências de mercado dão a ordem de grandeza para o cliente decidir.',
    itens: [
      'Coletar informações de projetos anteriores similares',
      'Ter referências de mercado e analisá-las',
      'Avaliação de custos diretos e indiretos',
    ],
  },
  {
    titulo: '4. Levantamento de Quantitativos',
    dica: 'Use as calculadoras deste app ou planilhas com composições de preços unitários.',
    itens: [
      'Ter todos os projetos aprovados em mãos',
      'Identificar as etapas e serviços do projeto',
      'Identificar as unidades de medida de cada serviço',
      'Elaborar a estrutura analítica do projeto (EAP)',
      'Elaborar planilha específica para memória de cálculo',
      'Separar os serviços conforme especificações técnicas',
      'Manter registros organizados com observações por etapa',
    ],
  },
  {
    titulo: '5. Mão de Obra',
    dica: 'Considere habilidades e especializações necessárias em cada fase do projeto.',
    itens: [
      'Identificação dos tipos de profissionais necessários',
      'Quantificação da mão de obra',
      'Avaliação do custo de cada categoria',
      'Levantamento dos custos associados (salários, encargos, benefícios)',
      'Análise da composição da equipe de trabalho',
    ],
  },
  {
    titulo: '6. Materiais e Cotação de Preços',
    dica: 'Cote com no mínimo 3 fornecedores já avaliados.',
    itens: [
      'Identificação das especificações dos materiais',
      'Relacionar todos os materiais necessários',
      'Disparar cotação para no mínimo 3 fornecedores avaliados',
      'Comparar tipo/prazo de entrega, custos e condições de pagamento',
      'Selecionar fornecedores pela relação custo-benefício',
    ],
  },
  {
    titulo: '7. Análise de Custos Indiretos',
    dica: 'Todo custo que não está diretamente na execução entra nesta etapa.',
    itens: [
      'Identificação dos custos indiretos (administrativo, escritório, comunicação)',
      'Avaliação das necessidades de seguro e taxas',
      'Estimativa de custos de transporte e armazenagem',
      'Inclusão de despesas gerais da obra (segurança, limpeza, energia, água)',
    ],
  },
  {
    titulo: '8. Análise de Curva ABC',
    dica: 'Negocie os itens da curva A para garantir o custo da obra.',
    itens: [
      'Elaborar a curva ABC de insumos e serviços',
      'Conferir todos os insumos/serviços da curva A',
      'Identificar itens a negociar para garantir o custo da obra',
      'Organizar por etapas e serviços para aquisição',
      'Revisão e validação do orçamento inicial',
    ],
  },
  {
    titulo: '9. Avaliação de Equipamentos',
    dica: 'Baseie-se nas especificações técnicas e na disponibilidade de recursos próprios ou locação.',
    itens: [
      'Levantamento dos equipamentos necessários para a obra',
      'Análise de equipamentos específicos e disponibilidade no mercado',
      'Análise da disponibilidade de equipamentos próprios',
      'Avaliação da necessidade de locação ou compra',
      'Consideração de prazos, forma de pagamento e exigências das locadoras',
    ],
  },
  {
    titulo: '10. Desenvolvimento do Cronograma',
    dica: 'Atribua prazos realistas e analise o caminho crítico.',
    itens: [
      'Utilizar a estrutura analítica do projeto no cronograma',
      'Alimentar o prazo em dias trabalhados de cada serviço',
      'Relacionar atividades antecessoras e sucessoras',
      'Analisar o caminho crítico para garantir o prazo estimado',
    ],
  },
  {
    titulo: '11. Avaliação de Riscos',
    dica: 'Identifique riscos potenciais e desenvolva estratégias de mitigação.',
    itens: [
      'Identificação dos principais riscos (clima, atrasos, mão de obra)',
      'Desenvolvimento de estratégias de mitigação',
      'Monitoramento contínuo dos riscos',
    ],
  },
  {
    titulo: '12. Análise de Viabilidade Financeira',
    dica: 'Trabalhe sempre com o cenário ideal e o pior cenário.',
    itens: [
      'Cálculo do retorno sobre investimento (ROI)',
      'Elaboração do fluxo de caixa projetado',
      'Análise de sensibilidade a variações de custos conforme o prazo',
      'Avaliação de indicadores financeiros',
      'Consideração de alternativas e cenários',
    ],
  },
  {
    titulo: '13. Revisão e Aprovação',
    dica: 'Revisão por pares e aprovação formal do cliente garantem qualidade e confiabilidade.',
    itens: [
      'Verificar se todas as etapas anteriores seguiram o escopo',
      'Revisar a precisão e a integridade de todas as informações',
      'Alinhamento com objetivos e requisitos do cliente',
      'Revisão minuciosa por pares e, quando necessário, por especialistas',
      'Obter aprovação formal do cliente após a revisão completa',
    ],
  },
];

const STORAGE_KEY = 'checklist-orcamento-v1';

export const ChecklistOrcamento: React.FC = () => {
  const [marcados, setMarcados] = useState<Record<string, boolean>>(() => {
    try {
      return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '{}');
    } catch {
      return {};
    }
  });

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(marcados));
  }, [marcados]);

  const chave = (s: number, i: number) => `${s}:${i}`;
  const toggle = (k: string) => setMarcados({ ...marcados, [k]: !marcados[k] });

  const progresso = useMemo(() => {
    const total = SECOES.reduce((acc, s) => acc + s.itens.length, 0);
    const feitos = SECOES.reduce(
      (acc, s, si) => acc + s.itens.filter((_, ii) => marcados[chave(si, ii)]).length,
      0
    );
    return { total, feitos, pct: total > 0 ? (feitos / total) * 100 : 0 };
  }, [marcados]);

  return (
    <div className="animate-fade-in" style={{ padding: '2rem', maxWidth: '1300px', margin: '0 auto' }}>
      <PageHeader
        icon={<ClipboardCheck color="var(--accent-primary)" />}
        titulo="Checklist de Orçamento"
        subtitulo="As 13 etapas de um orçamento de obras assertivo e lucrativo. Progresso salvo automaticamente."
      />

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 350px', gap: '2rem', alignItems: 'start' }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {SECOES.map((secao, si) => {
            const feitos = secao.itens.filter((_, ii) => marcados[chave(si, ii)]).length;
            const completa = feitos === secao.itens.length;
            return (
              <div key={secao.titulo} className="glass-panel" style={panelStyle}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                  <h2 style={{ fontSize: '1.1rem', color: completa ? 'var(--success)' : 'var(--text-primary)' }}>
                    {secao.titulo}
                  </h2>
                  <span style={{ fontSize: '0.85rem', color: completa ? 'var(--success)' : 'var(--text-muted)' }}>
                    {feitos}/{secao.itens.length}
                  </span>
                </div>
                <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '1rem', fontStyle: 'italic' }}>
                  💡 {secao.dica}
                </p>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem' }}>
                  {secao.itens.map((item, ii) => {
                    const k = chave(si, ii);
                    return (
                      <label
                        key={k}
                        style={{
                          display: 'flex', alignItems: 'flex-start', gap: '0.6rem', cursor: 'pointer',
                          color: marcados[k] ? 'var(--text-muted)' : 'var(--text-secondary)',
                          textDecoration: marcados[k] ? 'line-through' : 'none',
                          fontSize: '0.95rem',
                        }}
                      >
                        <input type="checkbox" checked={!!marcados[k]} onChange={() => toggle(k)} style={{ marginTop: '0.2rem' }} />
                        {item}
                      </label>
                    );
                  })}
                </div>
              </div>
            );
          })}
        </div>

        <div style={{ position: 'sticky', top: '2rem' }}>
          <div className="glass-panel" style={resumoPanelStyle}>
            <h2 style={{ fontSize: '1.2rem', marginBottom: '1.5rem', borderBottom: '1px solid rgba(255,255,255,0.1)', paddingBottom: '0.8rem' }}>
              Progresso Geral
            </h2>
            <div style={{ textAlign: 'center', marginBottom: '1rem' }}>
              <span style={{ fontSize: '2.5rem', fontWeight: 700, color: 'var(--success)' }}>{fmt(progresso.pct, 0)}%</span>
              <p style={{ color: 'var(--text-secondary)', marginTop: '0.3rem' }}>
                {progresso.feitos} de {progresso.total} itens concluídos
              </p>
            </div>
            <div style={{ height: '10px', borderRadius: '5px', background: 'rgba(255,255,255,0.1)', overflow: 'hidden' }}>
              <div
                style={{
                  height: '100%', width: `${progresso.pct}%`, transition: 'width 0.3s',
                  background: 'linear-gradient(90deg, var(--accent-primary), var(--success))',
                }}
              />
            </div>
            <button
              onClick={() => setMarcados({})}
              className="btn-secondary"
              style={{ width: '100%', marginTop: '1.5rem', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}
            >
              <RotateCcw size={16} /> Reiniciar checklist
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
