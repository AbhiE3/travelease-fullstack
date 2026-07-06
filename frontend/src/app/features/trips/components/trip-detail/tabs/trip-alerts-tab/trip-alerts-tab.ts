import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmBadgeImports } from '@spartan-ng/helm/badge';
import { alerts } from '@app/core/mock-data';

type Tone = 'destructive' | 'warning' | 'primary';

const TONE_CLASS: Record<Tone, string> = {
  destructive: 'bg-destructive/10 text-destructive border-destructive/20',
  warning: 'bg-warning/10 text-[oklch(0.40_0.12_75)] border-warning/20',
  primary: 'bg-primary/10 text-primary border-primary/20',
};

interface AlertView {
  id: string;
  title: string;
  desc: string;
  impact: string;
  action: string;
  level: string;
  toneClass: string;
}

function toneFor(level: string): Tone {
  if (level === 'Critical') return 'destructive';
  if (level === 'Medium') return 'warning';
  return 'primary';
}

@Component({
  selector: 'app-trip-alerts-tab',
  imports: [NgIcon, HlmCardImports, HlmBadgeImports],
  templateUrl: './trip-alerts-tab.html',
})
export class TripAlertsTab {
  public readonly alertViews: AlertView[] = alerts.map((a) => ({
    ...a,
    toneClass: TONE_CLASS[toneFor(a.level)],
  }));
}
