import {
  Component,
  DestroyRef,
  ElementRef,
  afterNextRender,
  effect,
  inject,
  input,
  untracked,
  viewChild,
} from '@angular/core';
import { dispose, init, use } from 'echarts/core';
import type { ECharts, EChartsCoreOption } from 'echarts/core';
import { SVGRenderer } from 'echarts/renderers';
import { BarChart, FunnelChart, HeatmapChart, LineChart } from 'echarts/charts';
import {
  CalendarComponent,
  GridComponent,
  TooltipComponent,
  VisualMapComponent,
} from 'echarts/components';

use([
  SVGRenderer,
  BarChart,
  FunnelChart,
  HeatmapChart,
  LineChart,
  CalendarComponent,
  GridComponent,
  TooltipComponent,
  VisualMapComponent,
]);

@Component({
  selector: 'app-echart',
  templateUrl: './echart.html',
})
export class EChart {
  public readonly options = input.required<EChartsCoreOption>();
  public readonly height = input('256px');

  private readonly container = viewChild.required<ElementRef<HTMLDivElement>>('container');
  private chart: ECharts | null = null;

  constructor() {
    const destroyRef = inject(DestroyRef);

    // React to option changes after the chart is initialized
    effect(() => {
      const options = this.options();
      // Use untracked so we don't accidentally track chart changes (though chart is not a signal, it's good practice)
      untracked(() => {
        if (this.chart) {
          this.chart.setOption(options, { notMerge: false });
        }
      });
    });

    afterNextRender(() => {
      const el = this.container().nativeElement;
      this.chart = init(el, undefined, { renderer: 'svg' });

      if (typeof ResizeObserver !== 'undefined') {
        let prevWidth: number;
        let prevHeight: number;

        const resizeObserver = new ResizeObserver((entries) => {
          const entry = entries[0];
          if (!entry) return;
          
          const { width, height } = entry.contentRect;

          // Ignore the first invocation from ResizeObserver to avoid interrupting the initial ECharts animation
          if (prevWidth === undefined && prevHeight === undefined) {
            prevWidth = width;
            prevHeight = height;
            return;
          }

          if (width !== prevWidth || height !== prevHeight) {
            prevWidth = width;
            prevHeight = height;
            this.chart?.resize();
          }
        });
        
        resizeObserver.observe(el);
        destroyRef.onDestroy(() => resizeObserver.disconnect());
      }

      destroyRef.onDestroy(() => {
        if (this.chart) {
          dispose(this.chart);
          this.chart = null;
        }
      });

      this.chart.setOption(this.options());
    });
  }
}
