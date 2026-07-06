import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideHome } from '@ng-icons/lucide';

@Component({
  selector: 'app-icon-test-host',
  imports: [NgIcon],
  template: `<ng-icon name="lucideHome" />`,
})
class IconTestHost {}

describe('icon provider', () => {
  it('renders a registered lucide icon as an inline SVG', async () => {
    await TestBed.configureTestingModule({
      imports: [IconTestHost],
      providers: [provideIcons({ lucideHome })],
    }).compileComponents();

    const fixture = TestBed.createComponent(IconTestHost);
    fixture.detectChanges();
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('svg')).not.toBeNull();
  });
});
