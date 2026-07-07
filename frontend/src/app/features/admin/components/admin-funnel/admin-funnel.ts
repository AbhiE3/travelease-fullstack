import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { EChart } from '@app/shared/ui/echart/echart';
import { CHART_COLORS } from '@app/shared/ui/echart/echart-theme';
import { buildRankingBarOption } from '@app/shared/ui/echart/ranking-bar-chart';
import { dropReasons, funnelStages } from '@app/core/mock-data';
import type { EChartsCoreOption } from 'echarts/core';

interface FunnelStageRow {
  stage: string;
  users: number;
  dropReason: string;
  widthPct: number;
  pctOfTotal: string;
  dropPct: string | null;
}

function buildStages(total: number): FunnelStageRow[] {
  return funnelStages.map((s, i) => {
    const prev = i > 0 ? funnelStages[i - 1].users : s.users;
    const dropPct = i > 0 ? (((prev - s.users) / prev) * 100).toFixed(1) : null;
    return {
      stage: s.stage,
      users: s.users,
      dropReason: s.dropReason,
      widthPct: (s.users / total) * 100,
      pctOfTotal: ((s.users / total) * 100).toFixed(0),
      dropPct,
    };
  });
}

export function buildFunnelOption(stages: FunnelStageRow[], color: string): EChartsCoreOption {
  return {
    animationDuration: 1800,
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        const stage = stages[params.dataIndex];
        const dropLine = stage.dropPct
          ? `<br/><span style="color:${CHART_COLORS.destructive}">${stage.dropPct}% drop-off — ${stage.dropReason}</span>`
          : '';
        return `<strong>${stage.stage}</strong><br/>${stage.users.toLocaleString()} users${dropLine}`;
      },
    },
    series: [
      {
        type: 'funnel',
        left: '10%',
        right: '10%',
        top: 8,
        bottom: 8,
        gap: 4,
        minSize: '40%',
        maxSize: '100%',
        sort: 'none',
        itemStyle: { color, borderColor: '#fff', borderWidth: 1 },
        label: { show: true, position: 'inside', formatter: '{b}\n{c}' },
        data: stages.map((s) => ({ name: s.stage, value: s.users })),
      },
    ],
  };
}

@Component({
  selector: 'app-admin-funnel',
  imports: [NgIcon, HlmCardImports, PageHeader, EChart],
  templateUrl: './admin-funnel.html',
})
export class AdminFunnel {
  public readonly total = funnelStages[0].users;
  public readonly completed = funnelStages[funnelStages.length - 1].users;
  public readonly conversion = ((this.completed / this.total) * 100).toFixed(1);
  public readonly totalDropOff = (100 - parseFloat(this.conversion)).toFixed(1);
  public readonly stages: FunnelStageRow[] = buildStages(this.total);
  public readonly dropReasons = dropReasons;

  public readonly funnelOptions: EChartsCoreOption = buildFunnelOption(this.stages, CHART_COLORS.primary);
  public readonly dropReasonsOptions: EChartsCoreOption = buildRankingBarOption(
    dropReasons.map((r) => ({ label: r.reason, value: r.pct })),
    CHART_COLORS.accent,
    '%',
  );
}
