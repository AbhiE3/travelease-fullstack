import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { PageHeader } from '@app/shared/ui/page-header/page-header';

@Component({
  selector: 'app-page-header-host',
  imports: [PageHeader],
  template: `
    <app-page-header title="My Trips" subtitle="All your trips">
      <button action>New Trip</button>
    </app-page-header>
  `,
})
class PageHeaderHost {}

@Component({
  selector: 'app-page-header-no-subtitle-host',
  imports: [PageHeader],
  template: `<app-page-header title="My Trips" />`,
})
class PageHeaderNoSubtitleHost {}

describe('PageHeader', () => {
  it('renders the title and subtitle', async () => {
    await TestBed.configureTestingModule({ imports: [PageHeaderHost] }).compileComponents();
    const fixture = TestBed.createComponent(PageHeaderHost);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('My Trips');
    expect(text).toContain('All your trips');
  });

  it('renders projected action content', async () => {
    await TestBed.configureTestingModule({ imports: [PageHeaderHost] }).compileComponents();
    const fixture = TestBed.createComponent(PageHeaderHost);
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('New Trip');
  });

  it('omits the subtitle paragraph when none is provided', async () => {
    await TestBed.configureTestingModule({ imports: [PageHeaderNoSubtitleHost] }).compileComponents();
    const fixture = TestBed.createComponent(PageHeaderNoSubtitleHost);
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('p')).toBeNull();
  });
});
