export default class InMemoryUserRepository {
  constructor() {
    this.users = [];
  }

  register(name, email, password) {
    this.users.push({ name, email, password });
    return Promise.resolve(undefined);
  }

  getRegisteredUsers() {
    return this.users;
  }
}
