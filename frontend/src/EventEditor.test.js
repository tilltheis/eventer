import React from 'react';
import { render, fireEvent } from '@testing-library/react';
import { MemoryRouter, Route } from 'react-router-dom';
import EventEditor from './EventEditor';
import InMemoryEventRepository from './test/InMemoryEventRepository';
import { flushPromises } from './test/testUtils';

// this cannot be inside of `beforeEach` or within an individual test or it won't work...
jest.mock('react-widgets/lib/DateTimePicker', () => props => (
  <input id={props.id} onChange={ev => props.onChange(new Date(ev.target.value))} />
));
// id will be set by the EventEditor to work around DropDownList impl limitations
jest.mock('react-widgets/lib/DropdownList', () => props => (
  <span id={`${props.id}_input`}>
    <input value={props.defaultValue} onChange={ev => props.onChange({ timeZone: ev.target.value })} />
  </span>
));

function resolvablePromise() {
  let resolvePromise;
  const promise = new Promise(resolve => {
    resolvePromise = resolve;
  });
  return [promise, resolvePromise];
}

const correctEvent = {
  id: 'test uuid',
  hostId: '6f31ccde-4321-4cc9-9056-6c3cbd550cba',
  title: 'test title',
  dateTime: '2020-01-25T13:36+02:00[Africa/Johannesburg]',
  description: 'test description',
};

test('calls repository with correct data and displays a toast and redirects to event list', async () => {
  const [redirectionPathPromise, resolveRedirectionPathPromise] = resolvablePromise();
  const [toastPromise, resolveToastPromise] = resolvablePromise();
  const eventRepository = new InMemoryEventRepository();
  const { container } = render(
    <MemoryRouter initialEntries={['/events/new']}>
      <Route
        path="/events/new"
        exact
        render={() => (
          <EventEditor
            generateUuid={() => 'test uuid'}
            setToast={resolveToastPromise}
            eventRepository={eventRepository}
          />
        )}
      />
      <Route
        path="*"
        render={props => {
          if (props.history.action === 'PUSH') resolveRedirectionPathPromise(props.history.location.pathname);
        }}
      />
    </MemoryRouter>,
  );

  expect(container.querySelector('form').checkValidity()).toBe(false);

  fireEvent.change(container.querySelector('#title'), {
    target: { value: 'test title' },
  });
  fireEvent.change(container.querySelector('#dateTime'), {
    target: { value: new Date('2020-01-25T13:36') },
  });
  fireEvent.change(container.querySelector('#timeZone'), {
    target: { value: 'Africa/Johannesburg' },
  });
  fireEvent.change(container.querySelector('#description'), {
    target: { value: 'test description' },
  });

  expect(container.querySelector('form').checkValidity()).toBe(true);

  fireEvent.click(container.querySelector('#createEvent'));

  expect(eventRepository.events).toEqual([correctEvent]);
  await expect(toastPromise).resolves.toBeDefined();
  await expect(redirectionPathPromise).resolves.toBe('/');
});

test('displays error message when data could not be submitted', async () => {
  const eventRepository = new InMemoryEventRepository({ simulateBrokenConnection: true });
  const { container } = render(
    <EventEditor generateUuid={() => 'test uuid'} setToast={() => {}} eventRepository={eventRepository} />,
  );

  fireEvent.change(container.querySelector('#title'), {
    target: { value: 'test title' },
  });
  fireEvent.change(container.querySelector('#dateTime'), {
    target: { value: new Date('2020-01-25T13:36') },
  });
  fireEvent.change(container.querySelector('#timeZone'), {
    target: { value: 'Africa/Johannesburg' },
  });
  fireEvent.change(container.querySelector('#description'), {
    target: { value: 'test description' },
  });

  expect(container.innerHTML).not.toMatch('danger');

  fireEvent.click(container.querySelector('#createEvent'));

  await flushPromises();
  expect(container.innerHTML).toMatch('danger');
});
