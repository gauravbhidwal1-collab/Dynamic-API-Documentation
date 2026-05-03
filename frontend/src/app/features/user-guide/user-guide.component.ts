import { Component } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardContent, MatCardHeader, MatCardSubtitle, MatCardTitle } from '@angular/material/card';
import { MatDivider } from '@angular/material/divider';
import { MatIcon } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-user-guide',
  standalone: true,
  imports: [MatCard, MatCardHeader, MatCardTitle, MatCardSubtitle, MatCardContent, MatDivider, MatButton, MatIcon, RouterLink],
  templateUrl: './user-guide.component.html',
  styleUrl: './user-guide.component.scss',
})
export class UserGuideComponent {
  readonly apiBaseUrl = environment.apiBaseUrl;
  /** Example path pattern (avoids raw `{` / `<` in template). */
  readonly pdfBackendPathExample = `${environment.apiBaseUrl}/api/pdf/{id}`;
  readonly pdfViewerQueryExample = '/pdf?apiId=1';

  printOrSavePdf(): void {
    window.print();
  }
}
