export interface Vaga {
  titulo: string;
  empresa: string;
  nivel: string;
  habilidades: string[];
  compatibilidade: number;
}

export interface Habilidade {
  nome: string;
  categoria: string;
  nivel: number;
}

export interface Treino {
  titulo: string;
  contexto: string;
  data: string;
  nota: number;
  duracao: string;
  pontosFortes?: string;
  melhorias?: string;
}

export const vagasIniciais: Vaga[] = [
  {
    titulo: "Desenvolvedor(a) Front-end",
    empresa: "Nubank",
    nivel: "Pleno",
    habilidades: ["Vue 3", "TypeScript", "Acessibilidade"],
    compatibilidade: 92,
  },
  {
    titulo: "Engenheiro(a) de Software",
    empresa: "iFood",
    nivel: "Sênior",
    habilidades: ["Java", "Microsserviços", "AWS"],
    compatibilidade: 78,
  },
  {
    titulo: "Desenvolvedor(a) Full Stack",
    empresa: "Loft",
    nivel: "Júnior",
    habilidades: ["Node.js", "Vue 3", "PostgreSQL"],
    compatibilidade: 84,
  },
];

export const habilidadesIniciais: Habilidade[] = [
  { nome: "Vue 3", categoria: "Front-end", nivel: 4 },
  { nome: "TypeScript", categoria: "Front-end", nivel: 3 },
  { nome: "JavaScript", categoria: "Linguagens", nivel: 4 },
  { nome: "Node.js", categoria: "Back-end", nivel: 3 },
  { nome: "CSS", categoria: "Front-end", nivel: 4 },
  { nome: "SQL", categoria: "Banco de dados", nivel: 2 },
];

export const historicoInicial: Treino[] = [
  {
    titulo: "Entrevista Front-end · Vue 3",
    contexto: "Habilidades: Vue 3, TypeScript",
    data: "Hoje, 10:42",
    nota: 86,
    duracao: "24 min",
  },
  {
    titulo: "Desenvolvimento Full Stack",
    contexto: "Vaga: Desenvolvedor(a) Full Stack · Loft",
    data: "12 out, 15:18",
    nota: 78,
    duracao: "31 min",
  },
  {
    titulo: "JavaScript e lógica",
    contexto: "Habilidades: JavaScript, lógica de programação",
    data: "08 out, 09:30",
    nota: 72,
    duracao: "19 min",
  },
];

export const perguntasDemonstracao = [
  "Conte um pouco sobre você e sua experiência com Vue 3. O que mais gosta de construir com essa tecnologia?",
  "Como você explicaria a diferença entre ref e reactive no Vue 3? Em que situação usaria cada um?",
  "Imagine que uma lista de produtos está renderizando lentamente. Como investigaria e melhoraria essa performance?",
];
