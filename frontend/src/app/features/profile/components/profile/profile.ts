import { Component } from '@angular/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmLabelImports } from '@spartan-ng/helm/label';
import { HlmAvatarImports } from '@spartan-ng/helm/avatar';
import { PageHeader } from '@app/shared/ui/page-header/page-header';

@Component({
  selector: 'app-profile',
  imports: [
    HlmCardImports,
    HlmButtonImports,
    HlmInputImports,
    HlmLabelImports,
    HlmAvatarImports,
    PageHeader,
  ],
  templateUrl: './profile.html',
})
export class Profile {
  protected readonly name = 'Sarathy R';
  protected readonly email = 'sarathy@example.com';
  protected readonly phone = '+91 9876543210';
  protected readonly defaultCity = 'Bengaluru';
}
