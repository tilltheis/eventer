export function getCookieValue(a) {
  const b = document.cookie.match(`(^|[^;]+)\\s*${a}\\s*=\\s*([^;]+)`);
  return b ? b.pop() : null;
}

// eg makeFsm('Foo', 'Bar') => { Foo: { name: 'Foo', isFoo: true, isBar: false}, ... }
export function makeFsm(...stateNames) {
  return Object.freeze(
    stateNames.reduce((states, name) => {
      const predicates = stateNames.reduce((preds, other) => ({ ...preds, [`is${other}`]: name === other }), {});
      return { ...states, [name]: { name, ...predicates } };
    }, {}),
  );
}

export const inputValueHandler = handler => event => handler(event.target.value);

const okOnly = response => (response.ok ? Promise.resolve(response) : Promise.reject(new Error('not ok')));

export function httpGet(url) {
  return fetch(url)
    .then(okOnly)
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
  }).then(okOnly);
}

export function httpPostIgnore(url, data) {
  return httpPost(url, data).then(undefined);
}

export function httpDelete(url) {
  return fetch(url, {
    method: 'DELETE',
    headers: {
      'X-Csrf-Token': getCookieValue('csrf-token'),
    },
  }).then(okOnly);
}

export function httpDeleteIgnore(url) {
  return httpDelete(url).then(undefined);
}
