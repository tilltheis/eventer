import { httpGet, httpPost } from './utils';

export default class EventRepository {
  create(event) {
    return httpPost('/events', event);
  }

  findAll() {
    return httpGet('/events');
  }
}
