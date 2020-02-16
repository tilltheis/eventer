import { httpPostIgnore, httpDeleteIgnore } from './utils';

export default class UserSessionRepository {
  login(email, password) {
    return httpPostIgnore('/sessions', { email, password });
  }

  logout() {
    return httpDeleteIgnore('/sessions');
  }
}
