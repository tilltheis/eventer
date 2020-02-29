import { httpPostIgnore } from './utils';

export default class UserRepository {
  register(name, email, password) {
    return httpPostIgnore('/users', { name, email, password });
  }
}
