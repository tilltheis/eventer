import { httpGet, httpPostIgnore } from './utils';

export default class EventRepository {
  create(event) {
    return httpPostIgnore('/events', event);
  }

  findAll() {
    return httpGet('/events');
  }
}
