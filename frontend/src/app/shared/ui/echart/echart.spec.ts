import { TestBed } from '@angular/core/testing';
import { getInstanceByDom } from 'echarts/core';
import { EChart } from '@app/shared/ui/echart/echart';

const BAR_OPTIONS = {
  xAxis: { type: 'category', data: ['a', 'b', 'c'] },
  yAxis: { type: 'value' },
  series: [{ type: 'bar', data: [1, 2, 3] }],
} as const;

describe('EChart', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [EChart] }).compileComponents();
  });

  it('initializes an ECharts instance on the container after render', async () => {
    const fixture = TestBed.createComponent(EChart);
    fixture.componentRef.setInput('options', BAR_OPTIONS);
    fixture.detectChanges();
    await fixture.whenStable();

    const container = fixture.nativeElement.querySelector('div') as HTMLDivElement;
    expect(getInstanceByDom(container)).toBeTruthy();
  });

  it('disposes the ECharts instance when the component is destroyed', async () => {
    const fixture = TestBed.createComponent(EChart);
    fixture.componentRef.setInput('options', BAR_OPTIONS);
    fixture.detectChanges();
    await fixture.whenStable();

    const container = fixture.nativeElement.querySelector('div') as HTMLDivElement;
    fixture.destroy();
    expect(getInstanceByDom(container)).toBeFalsy();
  });

  it('applies the height input as an inline style on the container', async () => {
    const fixture = TestBed.createComponent(EChart);
    fixture.componentRef.setInput('options', { series: [] });
    fixture.componentRef.setInput('height', '320px');
    fixture.detectChanges();
    await fixture.whenStable();

    const container = fixture.nativeElement.querySelector('div') as HTMLDivElement;
    expect(container.style.height).toBe('320px');
  });

  it('defaults height to 256px when not provided', async () => {
    const fixture = TestBed.createComponent(EChart);
    fixture.componentRef.setInput('options', { series: [] });
    fixture.detectChanges();
    await fixture.whenStable();

    const container = fixture.nativeElement.querySelector('div') as HTMLDivElement;
    expect(container.style.height).toBe('256px');
  });
});
