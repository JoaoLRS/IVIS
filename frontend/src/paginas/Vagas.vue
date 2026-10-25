<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import {
  ArrowRight,
  BriefcaseBusiness,
  Pencil,
  Plus,
  Search,
  Trash2,
} from "lucide-vue-next";
import { useRouter } from "vue-router";
import { useEstadoAplicacao } from "../estados/estadoAplicacao";
import CabecalhoPagina from "../componentes/CabecalhoPagina.vue";
import Etiqueta from "../componentes/Etiqueta.vue";
import ModalConfirmacao from "../componentes/ModalConfirmacao.vue";
import ModalFormulario from "../componentes/ModalFormulario.vue";

const estado = useEstadoAplicacao();
const roteador = useRouter();
const consulta = ref("");
const modal = ref(false);
const indiceParaExcluir = ref<number | null>(null);
const indiceEmEdicao = ref<number | null>(null);
const formulario = reactive({
  titulo: "",
  empresa: "",
  nivel: "Júnior",
  habilidades: "",
});
const emit = defineEmits<{ notificar: [mensagem: string] }>();
const vagasFiltradas = computed(() =>
  estado.vagas
    .map((vaga, indice) => ({ vaga, indice }))
    .filter(({ vaga }) =>
      `${vaga.titulo} ${vaga.empresa} ${vaga.habilidades.join(" ")}`
        .toLowerCase()
        .includes(consulta.value.toLowerCase()),
    ),
);
const vagaParaExcluir = computed(() =>
  indiceParaExcluir.value === null
    ? null
    : (estado.vagas[indiceParaExcluir.value] ?? null),
);

function abrirNova() {
  indiceEmEdicao.value = null;
  Object.assign(formulario, {
    titulo: "",
    empresa: "",
    nivel: "Júnior",
    habilidades: "",
  });
  modal.value = true;
}

function abrirEdicao(indice: number) {
  indiceEmEdicao.value = indice;
  const vaga = estado.vagas[indice];
  Object.assign(formulario, {
    titulo: vaga.titulo,
    empresa: vaga.empresa,
    nivel: vaga.nivel,
    habilidades: vaga.habilidades.join(", "),
  });
  modal.value = true;
}

function salvar() {
  const anterior =
    indiceEmEdicao.value === null
      ? undefined
      : estado.vagas[indiceEmEdicao.value];
  const vaga = {
    titulo: formulario.titulo.trim(),
    empresa: formulario.empresa.trim(),
    nivel: formulario.nivel,
    habilidades: formulario.habilidades
      .split(",")
      .map((item) => item.trim())
      .filter(Boolean),
    compatibilidade: anterior?.compatibilidade ?? 76,
  };
  if (indiceEmEdicao.value === null) {
    estado.adicionarVaga(vaga);
    emit("notificar", "Vaga adicionada.");
  } else {
    estado.editarVaga(indiceEmEdicao.value, vaga);
    emit("notificar", "Vaga atualizada.");
  }
  modal.value = false;
}

function remover(indice: number) {
  const vaga = estado.vagas[indice];
  if (estado.vagaEmTreinamento === vaga.titulo) {
    emit(
      "notificar",
      "Esta vaga está vinculada ao treino em andamento e não pode ser excluída.",
    );
    return;
  }
  estado.removerVaga(indice);
  emit("notificar", "Vaga removida.");
}

function solicitarRemocao(indice: number) {
  const vaga = estado.vagas[indice];
  if (!vaga) return;
  if (estado.vagaEmTreinamento === vaga.titulo) {
    emit(
      "notificar",
      "Esta vaga está vinculada ao treino em andamento e não pode ser excluída.",
    );
    return;
  }
  indiceParaExcluir.value = indice;
}

function confirmarRemocao() {
  if (indiceParaExcluir.value === null) return;
  remover(indiceParaExcluir.value);
  indiceParaExcluir.value = null;
}

function treinarPara(titulo: string) {
  const vaga = estado.vagas.find((item) => item.titulo === titulo);
  if (!vaga) return;
  roteador.push({ path: "/treinos", query: { vaga: vaga.titulo } });
}
</script>

<template>
  <CabecalhoPagina
    titulo="Vagas de interesse"
    descricao="Cadastre e organize vagas para orientar seus treinamentos de entrevista."
  >
    <button class="btn btn-primary" @click="abrirNova">
      <Plus :size="16" /> Adicionar vaga
    </button>
  </CabecalhoPagina>
  <div class="alert opportunity-alert d-flex align-items-center gap-3">
    <span class="alert-icon"><BriefcaseBusiness :size="19" /></span>
    <div>
      <strong>{{ estado.vagas.length }} vagas cadastradas</strong>
      <p class="mb-0">
        As informações da vaga serão usadas como contexto de uma entrevista
        demonstrativa.
      </p>
    </div>
  </div>
  <div
    class="d-flex justify-content-between align-items-center gap-3 mb-3 flex-wrap"
  ></div>
  <div class="row g-3">
    <div
      v-for="{ vaga, indice } in vagasFiltradas"
      :key="`${vaga.titulo}-${indice}`"
      class="col-md-6 col-xl-4"
    >
      <article class="card job-card h-100">
        <div class="card-body d-flex flex-column">
          <div class="d-flex justify-content-end align-items-center">
            <div class="d-flex gap-2">
              <button
                class="btn btn-sm btn-outline-secondary"
                :aria-label="`Editar vaga ${vaga.titulo}`"
                @click="abrirEdicao(indice)"
              >
                <Pencil :size="14" /></button
              ><button
                class="btn btn-sm btn-outline-danger"
                :aria-label="`Excluir vaga ${vaga.titulo}`"
                :disabled="estado.vagaEmTreinamento === vaga.titulo"
                :title="
                  estado.vagaEmTreinamento === vaga.titulo
                    ? 'Vaga usada no treino em andamento'
                    : 'Excluir vaga'
                "
                @click="solicitarRemocao(indice)"
              >
                <Trash2 :size="14" />
              </button>
            </div>
          </div>
          <div class="company-meta mt-3">
            {{ vaga.empresa }} · {{ vaga.nivel }}
          </div>
          <h2 class="job-title">{{ vaga.titulo }}</h2>
          <div class="d-flex flex-wrap gap-2">
            <Etiqueta
              v-for="habilidade in vaga.habilidades"
              :key="habilidade"
              >{{ habilidade }}</Etiqueta
            >
          </div>
          <div class="mt-auto">
            <div class="match-label mt-4">
              <span>Compatibilidade demonstrativa</span
              ><strong>{{ vaga.compatibilidade }}%</strong>
            </div>
            <div class="progress mb-3">
              <div
                class="progress-bar"
                :style="{ width: `${vaga.compatibilidade}%` }"
              />
            </div>
            <button
              class="btn btn-primary w-100"
              @click="treinarPara(vaga.titulo)"
            >
              Treinar para esta vaga <ArrowRight :size="14" />
            </button>
          </div>
        </div>
      </article>
    </div>
    <div v-if="!vagasFiltradas.length" class="col-12">
      <div class="empty-state card card-body">Nenhuma vaga encontrada.</div>
    </div>
  </div>
  <ModalFormulario
    v-if="modal"
    :titulo="
      indiceEmEdicao === null
        ? 'Adicionar vaga de interesse'
        : 'Editar vaga de interesse'
    "
    :rotulo-acao="indiceEmEdicao === null ? 'Adicionar' : 'Salvar alterações'"
    @fechar="modal = false"
    @enviar="salvar"
  >
    <label class="form-label"
      >Nome do cargo<input
        v-model="formulario.titulo"
        class="form-control"
        required
        placeholder="Ex.: Desenvolvedor(a) front-end"
    /></label>
    <label class="form-label"
      >Empresa<input
        v-model="formulario.empresa"
        class="form-control"
        required
        placeholder="Ex.: Nubank"
    /></label>
    <label class="form-label"
      >Nível<select v-model="formulario.nivel" class="form-select">
        <option>Júnior</option>
        <option>Pleno</option>
        <option>Sênior</option>
      </select></label
    >
    <label class="form-label"
      >Habilidades e requisitos<input
        v-model="formulario.habilidades"
        class="form-control"
        required
        placeholder="Vue, TypeScript, CSS"
      /><small class="form-text">Separe os itens por vírgula.</small></label
    >
  </ModalFormulario>
  <ModalConfirmacao
    v-if="vagaParaExcluir"
    titulo="Excluir vaga"
    :mensagem="`Tem certeza de que deseja excluir a vaga ${vagaParaExcluir.titulo}?`"
    @fechar="indiceParaExcluir = null"
    @confirmar="confirmarRemocao"
  />
</template>
