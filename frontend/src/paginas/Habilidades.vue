<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { Code2, Pencil, Plus, Trash2 } from "lucide-vue-next";
import { useEstadoAplicacao } from "../estados/estadoAplicacao";
import CabecalhoPagina from "../componentes/CabecalhoPagina.vue";
import Etiqueta from "../componentes/Etiqueta.vue";
import ModalConfirmacao from "../componentes/ModalConfirmacao.vue";
import ModalFormulario from "../componentes/ModalFormulario.vue";

const estado = useEstadoAplicacao();
const modal = ref(false);
const indiceParaExcluir = ref<number | null>(null);
const indiceEmEdicao = ref<number | null>(null);
const formulario = reactive({ nome: "", categoria: "", nivel: 3 });
const niveis = [
  "",
  "Iniciante",
  "Básico",
  "Intermediário",
  "Avançado",
  "Especialista",
];
const emit = defineEmits<{ notificar: [mensagem: string] }>();
const habilidadeParaExcluir = computed(() =>
  indiceParaExcluir.value === null
    ? null
    : (estado.habilidades[indiceParaExcluir.value] ?? null),
);

function abrirNovo() {
  indiceEmEdicao.value = null;
  Object.assign(formulario, { nome: "", categoria: "", nivel: 3 });
  modal.value = true;
}

function abrirEdicao(indice: number) {
  indiceEmEdicao.value = indice;
  Object.assign(formulario, estado.habilidades[indice]);
  modal.value = true;
}

function salvar() {
  const habilidade = { ...formulario };
  if (indiceEmEdicao.value === null) {
    estado.adicionarHabilidade(habilidade);
    emit("notificar", "Habilidade adicionada ao perfil.");
  } else {
    estado.editarHabilidade(indiceEmEdicao.value, habilidade);
    emit(
      "notificar",
      "Habilidade atualizada. Ela será considerada nos próximos treinos.",
    );
  }
  modal.value = false;
}

function confirmarRemocao() {
  const indice = indiceParaExcluir.value;
  if (indice === null) return;

  const habilidade = estado.habilidades[indice];
  if (habilidade) {
    estado.removerHabilidade(indice);
    emit("notificar", `Habilidade ${habilidade.nome} removida do perfil.`);
  }
  indiceParaExcluir.value = null;
}
</script>

<template>
  <CabecalhoPagina
    titulo="Minhas habilidades"
    descricao="Cadastre e mantenha atualizadas as habilidades usadas para personalizar os próximos treinamentos."
  >
    <button class="btn btn-primary" @click="abrirNovo">
      <Plus :size="16" /> Adicionar habilidade
    </button>
  </CabecalhoPagina>
  <div class="alert skill-alert">
    <strong>Perfil de habilidades</strong>
    <p class="mb-0">
      As alterações feitas aqui serão usadas como contexto quando você iniciar
      um novo treino por habilidades.
    </p>
  </div>
  <div class="d-flex align-items-center justify-content-between my-3">
    <h2 class="panel-title mb-0">
      Habilidades cadastradas
      <span class="text-secondary fw-normal"
        >({{ estado.habilidades.length }})</span
      >
    </h2>
  </div>
  <div v-if="estado.habilidades.length" class="row g-3">
    <div
      v-for="(habilidade, indice) in estado.habilidades"
      :key="`${habilidade.nome}-${indice}`"
      class="col-sm-6 col-xl-4"
    >
      <article class="card skill-card h-100">
        <div class="card-body">
          <div class="d-flex justify-content-between align-items-center">
            <span class="skill-icon"><Code2 :size="18" /></span>
            <div class="d-flex gap-2">
              <button
                class="btn btn-sm btn-outline-secondary"
                :aria-label="`Editar ${habilidade.nome}`"
                @click="abrirEdicao(indice)"
              >
                <Pencil :size="15" /></button
              ><button
                class="btn btn-sm btn-outline-danger"
                :aria-label="`Remover ${habilidade.nome}`"
                @click="indiceParaExcluir = indice"
              >
                <Trash2 :size="15" />
              </button>
            </div>
          </div>
          <h3 class="skill-title">{{ habilidade.nome }}</h3>
          <Etiqueta>{{ habilidade.categoria }}</Etiqueta>
          <div class="skill-level mt-4">
            <div class="d-flex justify-content-between">
              <span>Nível atual</span
              ><strong>{{ niveis[habilidade.nivel] }}</strong>
            </div>
            <div class="level-bars">
              <i
                v-for="n in 5"
                :key="n"
                :class="{ filled: n <= habilidade.nivel }"
              />
            </div>
          </div>
        </div>
      </article>
    </div>
  </div>
  <div v-else class="empty-state card card-body">
    Nenhuma habilidade cadastrada. Adicione habilidades para usá-las nos
    próximos treinamentos.
  </div>
  <ModalFormulario
    v-if="modal"
    :titulo="
      indiceEmEdicao === null ? 'Adicionar habilidade' : 'Editar habilidade'
    "
    :rotulo-acao="indiceEmEdicao === null ? 'Adicionar' : 'Salvar alterações'"
    @fechar="modal = false"
    @enviar="salvar"
  >
    <label class="form-label"
      >Nome da habilidade<input
        v-model="formulario.nome"
        class="form-control"
        required
        placeholder="Ex.: Vue.js"
    /></label>
    <label class="form-label"
      >Categoria<input
        v-model="formulario.categoria"
        class="form-control"
        required
        placeholder="Ex.: Front-end"
    /></label>
    <label class="form-label"
      >Nível atual<select v-model.number="formulario.nivel" class="form-select">
        <option :value="1">Iniciante</option>
        <option :value="2">Básico</option>
        <option :value="3">Intermediário</option>
        <option :value="4">Avançado</option>
        <option :value="5">Especialista</option>
      </select></label
    >
  </ModalFormulario>
  <ModalConfirmacao
    v-if="habilidadeParaExcluir"
    titulo="Excluir habilidade?"
    :mensagem="`Tem certeza de que deseja excluir a habilidade ${habilidadeParaExcluir.nome}?`"
    @fechar="indiceParaExcluir = null"
    @confirmar="confirmarRemocao"
  />
</template>
