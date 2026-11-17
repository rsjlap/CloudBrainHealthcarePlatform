<script setup lang="ts">
import * as echarts from 'echarts'
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'

const props = defineProps<{
  option: echarts.EChartsCoreOption
}>()

const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null
let resizeObserver: ResizeObserver | null = null

function render() {
  if (!chartRef.value) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }
  chart.setOption(props.option, true)
  nextTick(() => chart?.resize())
}

function onResize() {
  chart?.resize()
}

watch(() => props.option, render, { deep: true })

onMounted(() => {
  render()
  window.addEventListener('resize', onResize)
  if (chartRef.value) {
    resizeObserver = new ResizeObserver(onResize)
    resizeObserver.observe(chartRef.value)
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  resizeObserver?.disconnect()
  resizeObserver = null
  chart?.dispose()
  chart = null
})

defineExpose({ resize: onResize })
</script>

<template>
  <div ref="chartRef" class="echart-box" />
</template>

<style scoped>
.echart-box {
  width: 100%;
  height: 320px;
}
</style>
