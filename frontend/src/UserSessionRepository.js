import { httpPost, httpDelete } from './utils';

export default class UserSessionRepository {
  login(email, password) {
    return httpPost('/sessions', { email, password });
  }

  logout() {
    return httpDelete('/sessions');
  }
}
