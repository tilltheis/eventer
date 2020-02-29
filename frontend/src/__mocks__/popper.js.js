// This mock is necessary to avoid error logs in tests (see
// https://github.com/react-bootstrap/react-bootstrap/issues/4997#issuecomment-589784794).
import PopperJs from 'popper.js';

export default class Popper {
  static placements = PopperJs.placements;

  constructor() {
    return {
      destroy: () => {},
      scheduleUpdate: () => {},
    };
  }
}
