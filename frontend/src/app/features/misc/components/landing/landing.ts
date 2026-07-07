import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import { HlmButtonImports } from '@spartan-ng/helm/button';

interface FeatureCard {
  title: string;
  desc: string;
  icon: string;
}

const FEATURES: FeatureCard[] = [
  { title: 'Invite & coordinate', desc: 'Roles, RSVPs, group chat.', icon: 'lucideUsers' },
  { title: 'Book together', desc: 'Adjacent seats, group hotels.', icon: 'lucideBus' },
  { title: 'Split expenses', desc: 'Splitwise-style settlements.', icon: 'lucideWallet' },
  {
    title: 'Disruption handled',
    desc: 'Live delays and rescheduling.',
    icon: 'lucideShieldCheck',
  },
];

@Component({
  selector: 'app-landing',
  imports: [RouterLink, NgIcon, HlmButtonImports],
  templateUrl: './landing.html',
})
export class Landing {
  public readonly features = FEATURES;
}
