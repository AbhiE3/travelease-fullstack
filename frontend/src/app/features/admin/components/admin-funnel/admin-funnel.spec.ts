import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideTrendingDown } from '@ng-icons/lucide';
import { dropReasons, funnelStages } from '@app/core/mock-data';
import { AdminFunnel, buildFunnelOption } from '@app/features/admin/components/admin-funnel/admin-funnel';

describe('AdminFunnel', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminFunnel],
      providers: [provideIcons({ lucideTrendingDown })],
    }).compileComponents();
  });

  it('computes conversion and total drop-off correctly', () => {
    const fixture = TestBed.createComponent(AdminFunnel);
    const c = fixture.componentInstance;
    const total = funnelStages[0].users;
    const completed = funnelStages[funnelStages.length - 1].users;
    const conversion = ((completed / total) * 100).toFixed(1);
    expect(c.conversion).toBe(conversion);
    expect(c.totalDropOff).toBe((100 - parseFloat(conversion)).toFixed(1));
  });

  it('computes the drop-off percentage between each consecutive stage pair', () => {
    const fixture = TestBed.createComponent(AdminFunnel);
    const c = fixture.componentInstance;
    for (let i = 1; i < funnelStages.length; i++) {
      const prev = funnelStages[i - 1].users;
      const curr = funnelStages[i].users;
      const expected = (((prev - curr) / prev) * 100).toFixed(1);
      expect(c.stages[i].dropPct).toBe(expected);
    }
    expect(c.stages[0].dropPct).toBeNull();
  });

  it('builds a drop reasons chart with all 5 reasons and percentages', () => {
    const fixture = TestBed.createComponent(AdminFunnel);
    const c = fixture.componentInstance;
    const yAxis = c.dropReasonsOptions['yAxis'] as { data: string[] };
    const series = (c.dropReasonsOptions['series'] as any[])[0];

    for (const r of dropReasons) {
      const idx = yAxis.data.indexOf(r.reason);
      expect(idx).toBeGreaterThanOrEqual(0);
      expect(series.data[idx]).toBe(r.pct);
    }
  });
});

describe('buildFunnelOption', () => {
  const stages = funnelStages.map((s, i) => ({
    stage: s.stage,
    users: s.users,
    dropReason: s.dropReason,
    widthPct: 0,
    pctOfTotal: '0',
    dropPct: i > 0 ? '10.0' : null,
  }));

  it('maps each stage to a funnel data point with its stage name and user count', () => {
    const option = buildFunnelOption(stages, 'oklch(0.5 0.1 200)');
    const series = (option['series'] as any[])[0];
    expect(series.data).toEqual(stages.map((s) => ({ name: s.stage, value: s.users })));
  });

  it('includes the drop-off percentage and reason in the tooltip for stages after the first', () => {
    const option = buildFunnelOption(stages, 'oklch(0.5 0.1 200)');
    const tooltipFormatter = (option['tooltip'] as any).formatter as (p: any) => string;
    const html = tooltipFormatter({ dataIndex: 1 });
    expect(html).toContain('10.0% drop-off');
    expect(html).toContain(stages[1].dropReason);
  });

  it('omits the drop-off line in the tooltip for the first stage', () => {
    const option = buildFunnelOption(stages, 'oklch(0.5 0.1 200)');
    const tooltipFormatter = (option['tooltip'] as any).formatter as (p: any) => string;
    const html = tooltipFormatter({ dataIndex: 0 });
    expect(html).not.toContain('drop-off');
  });
});
