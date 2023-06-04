<template>
  <main>
    <b-button @click="action">Action</b-button>
    <p>id : {{ $route.params.id }}</p>

  </main>
</template>

<script setup>

import {getCurrentInstance} from "vue";

const app = getCurrentInstance()

function action() {
  app.ctx._bv__toast.toast("some")
}

</script>
