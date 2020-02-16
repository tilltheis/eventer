export default class InMemoryEventRepository {
  // eg new InMemoryEventRepository({ simulateBrokenConnection: true, initialEvents: [{id: 'abc', title: 'def', ...}] })
  constructor(options) {
    this.events = (options && options.initialEvents) || [];

    const simulateBrokenConnection = options && options.simulateBrokenConnection;
    this.connection = f => (simulateBrokenConnection ? Promise.reject(new Error('broken connection')) : f());
  }

  create(event) {
    return this.connection(() => {
      this.events.push(event);
      return Promise.resolve(undefined);
    });
  }

  findAll() {
    return this.connection(() => {
      return Promise.resolve(this.events);
    });
  }
}
