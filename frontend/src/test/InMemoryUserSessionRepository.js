export default class InMemoryUserSessionRepository {
  // eg new InMemoryUserSessionRepository(
  //   [{ name: 'foo', email: 'bar', password: 'baz' })],
  //   { initialLoggedInUser: [{ name: 'foo', email: 'bar', password: 'baz' }, simulateBrokenConnection: true }
  // )
  constructor(allUsers, options) {
    this.allUsers = allUsers;
    this.loggedInUser = (options && options.initialLoggedInUser) || null;

    const simulateBrokenConnection = options && options.simulateBrokenConnection;
    this.connection = (f) => (simulateBrokenConnection ? Promise.reject(new Error('broken connection')) : f());
  }

  login(email, password) {
    return this.connection(() => {
      const foundUser = this.allUsers.find((user) => user.email === email && user.password === password);
      if (foundUser !== undefined) {
        this.loggedInUser = foundUser;
        return Promise.resolve(undefined);
      }
      return Promise.reject(new Error('not ok'));
    });
  }

  logout() {
    return this.connection(() => {
      this.loggedInUser = null;
      return Promise.resolve(undefined);
    });
  }

  getLoggedInUser() {
    return this.loggedInUser;
  }
}
