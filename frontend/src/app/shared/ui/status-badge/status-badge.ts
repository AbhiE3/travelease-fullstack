import { Component, computed, input } from '@angular/core';
import { HlmBadgeImports } from '@spartan-ng/helm/badge';

const STATUS_CLASS_MAP: Record<string, string> = {
  Accepted: 'bg-success/15 text-success border-success/20',
  Pending: 'bg-warning/15 text-[oklch(0.45_0.12_75)] border-warning/20',
  Rejected: 'bg-destructive/15 text-destructive border-destructive/20',
  Paid: 'bg-success/15 text-success border-success/20',
  Confirmed: 'bg-success/10 text-success border-success/20',
  Active: 'bg-success/10 text-success border-success/20',
  Maintenance: 'bg-warning/15 text-[oklch(0.45_0.12_75)] border-warning/20',
  upcoming: 'bg-primary/10 text-primary border-primary/20',
  planning: 'bg-warning/15 text-[oklch(0.45_0.12_75)] border-warning/20',
  ongoing: 'bg-accent/15 text-accent border-accent/20',
  completed: 'bg-muted text-muted-foreground border-border',
  PLANNING: 'bg-warning/15 text-[oklch(0.45_0.12_75)] border-warning/20',
  CONFIRMED: 'bg-success/10 text-success border-success/20',
  ONGOING: 'bg-accent/15 text-accent border-accent/20',
  COMPLETED: 'bg-muted text-muted-foreground border-border',
  CANCELLED: 'bg-destructive/15 text-destructive border-destructive/20',
  INVITED: 'bg-warning/15 text-[oklch(0.45_0.12_75)] border-warning/20',
  ACCEPTED: 'bg-success/10 text-success border-success/20',
  REJECTED: 'bg-destructive/15 text-destructive border-destructive/20',
};

@Component({
  selector: 'app-status-badge',
  imports: [HlmBadgeImports],
  template: `<span hlmBadge variant="outline" [class]="badgeClass()">{{ status() }}</span>`,
})
export class StatusBadge {
  public readonly status = input.required<string>();

  protected readonly badgeClass = computed(
    () => `${STATUS_CLASS_MAP[this.status()] ?? ''} capitalize font-medium`,
  );
}
