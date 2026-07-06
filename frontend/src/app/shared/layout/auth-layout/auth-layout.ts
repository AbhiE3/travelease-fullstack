import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-auth-layout',
  imports: [RouterLink, RouterOutlet],
  templateUrl: './auth-layout.html',
})
export class AuthLayout {}
