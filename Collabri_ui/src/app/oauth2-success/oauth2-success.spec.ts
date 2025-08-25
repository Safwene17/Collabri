import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Oauth2Success } from './oauth2-success';

describe('Oauth2Success', () => {
  let component: Oauth2Success;
  let fixture: ComponentFixture<Oauth2Success>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Oauth2Success]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Oauth2Success);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
