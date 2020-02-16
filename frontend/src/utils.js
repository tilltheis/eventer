export function getCookieValue(a) {
  const b = document.cookie.match(`(^|[^;]+)\\s*${a}\\s*=\\s*([^;]+)`);
  return b ? b.pop() : null;
}

// eg makeFsm('Foo', 'Bar') => { Foo: { name: 'Foo', isFoo: true, isBar: false}, ... }
export function makeFsm(...stateNames) {
  return Object.freeze(
    stateNames.reduce((states, x) => {
      const predicates = stateNames.reduce((preds, y) => ({ ...preds, [`is${y}`]: x === y }), {});
      return { ...states, [x]: { name: x, ...predicates } };
    }, {}),
  );
}

const okOnlyResponse = response => (response.ok ? Promise.resolve(response) : Promise.reject(new Error('not ok')));
const okOnlyUndefined = response => okOnlyResponse(response).then(undefined);

export function httpGet(url) {
  return fetch(url)
    .then(okOnlyResponse)
    .then(response => response.json());
}

export function httpPost(url, data) {
  return fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Csrf-Token': getCookieValue('csrf-token'),
    },
    body: JSON.stringify(data),
  }).then(okOnlyUndefined);
}

export function httpDelete(url) {
  return fetch(url, {
    method: 'DELETE',
    headers: {
      'X-Csrf-Token': getCookieValue('csrf-token'),
    },
  }).then(okOnlyUndefined);
}
