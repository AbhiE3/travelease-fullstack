import { CHART_COLORS } from '@app/shared/ui/echart/echart-theme';

describe('CHART_COLORS', () => {
  it('matches the light-theme CSS variable values defined in styles.css', () => {
    expect(CHART_COLORS.primary).toBe('oklch(0.50 0.11 215)');
    expect(CHART_COLORS.success).toBe('oklch(0.65 0.15 155)');
    expect(CHART_COLORS.destructive).toBe('oklch(0.60 0.22 25)');
    expect(CHART_COLORS.warning).toBe('oklch(0.78 0.15 75)');
    expect(CHART_COLORS.accent).toBe('oklch(0.72 0.14 40)');
    expect(CHART_COLORS.muted).toBe('oklch(0.96 0.01 220)');
    expect(CHART_COLORS.mutedForeground).toBe('oklch(0.50 0.03 230)');
  });
});
