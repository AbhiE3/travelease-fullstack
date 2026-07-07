import type { EChartsCoreOption } from 'echarts/core';

export interface RankingBarItem {
  label: string;
  value: number;
}

export function buildRankingBarOption(
  items: RankingBarItem[],
  color: string,
  unit: '%' | '' = '',
): EChartsCoreOption {
  const reversed = [...items].reverse();

  return {
    animationDuration: 1800,
    grid: { left: 100, right: 40, top: 8, bottom: 8, containLabel: true },
    xAxis: { type: 'value', show: false },
    yAxis: {
      type: 'category',
      data: reversed.map((i) => i.label),
      axisLine: { show: false },
      axisTick: { show: false },
    },
    tooltip: { trigger: 'axis', valueFormatter: (v: number) => `${v}${unit}` },
    series: [
      {
        type: 'bar',
        data: reversed.map((i) => i.value),
        barWidth: 14,
        itemStyle: { color, borderRadius: [0, 4, 4, 0] },
        label: { show: true, position: 'right', formatter: `{c}${unit}` },
      },
    ],
  };
}
