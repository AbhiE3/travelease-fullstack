import { buildRankingBarOption } from '@app/shared/ui/echart/ranking-bar-chart';

describe('buildRankingBarOption', () => {
  const items = [
    { label: 'Goa', value: 92 },
    { label: 'Manali', value: 74 },
  ];

  it('puts the first item at the top of the category axis (reversed for ECharts bottom-up rendering)', () => {
    const option = buildRankingBarOption(items, 'oklch(0.5 0.1 200)');
    const yAxis = option['yAxis'] as { data: string[] };
    expect(yAxis.data).toEqual(['Manali', 'Goa']);
  });

  it('reverses the series data to match the reversed category order', () => {
    const option = buildRankingBarOption(items, 'oklch(0.5 0.1 200)');
    const series = (option['series'] as any[])[0];
    expect(series.data).toEqual([74, 92]);
  });

  it('applies the given color to the bar itemStyle', () => {
    const option = buildRankingBarOption(items, 'oklch(0.5 0.1 200)');
    const series = (option['series'] as any[])[0];
    expect(series.itemStyle.color).toBe('oklch(0.5 0.1 200)');
  });

  it('formats labels with the given unit suffix', () => {
    const option = buildRankingBarOption(items, 'oklch(0.5 0.1 200)', '%');
    const series = (option['series'] as any[])[0];
    expect(series.label.formatter).toBe('{c}%');
  });

  it('defaults to no unit suffix', () => {
    const option = buildRankingBarOption(items, 'oklch(0.5 0.1 200)');
    const series = (option['series'] as any[])[0];
    expect(series.label.formatter).toBe('{c}');
  });

  it('does not mutate the input items array', () => {
    const original = [...items];
    buildRankingBarOption(items, 'oklch(0.5 0.1 200)');
    expect(items).toEqual(original);
  });
});
