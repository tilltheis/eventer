import React from 'react';
import { render, fireEvent, act } from '@testing-library/react';
import { MemoryRouter, Route } from 'react-router-dom';
import EventEditor from './EventEditor';
import InMemoryEventRepository from './test/InMemoryEventRepository';

// this cannot be inside of `beforeEach` or within an individual test or it won't work...
jest.mock('react-widgets/lib/DateTimePicker', () => (props) => (
  <input id={`${props.id}_input`} onChange={(ev) => props.onChange(new Date(ev.target.value))} />
));
// id will be set by the EventEditor to work around DropDownList impl limitations
jest.mock('react-widgets/lib/DropdownList', () => (props) => (
  <span id={`${props.id}_input`}>
    <input value={props.defaultValue} onChange={(ev) => props.onChange({ timeZone: ev.target.value })} />
  </span>
));

function resolvablePromise() {
  let resolvePromise;
  const promise = new Promise((resolve) => {
    resolvePromise = resolve;
  });
  return [promise, resolvePromise];
}

const correctEvent = {
  title: 'test title',
  dateTime: '2020-01-25T13:36+02:00[Africa/Johannesburg]',
  description: 'test description',
};

test('calls repository with correct data and displays a toast and redirects to event list', async () => {
  const [redirectionPathPromise, resolveRedirectionPathPromise] = resolvablePromise();
  const [toastPromise, resolveToastPromise] = resolvablePromise();
  const eventRepository = new InMemoryEventRepository();
  const { getByLabelText, getByText } = render(
    <MemoryRouter initialEntries={['/events/new']}>
      <Route
        exact
        path="/events/new"
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
        render={(props) => {
          if (props.history.action === 'PUSH') resolveRedirectionPathPromise(props.history.location.pathname);
        }}
      />
    </MemoryRouter>,
  );

  expect(getByText('Create Event').form.checkValidity()).toBe(false);

  await act(async () => {
    fireEvent.change(getByLabelText('Title'), {
      target: { value: 'test title' },
    });
    fireEvent.change(getByLabelText('Date & Time'), {
      target: { value: new Date('2020-01-25T13:36') },
    });
    fireEvent.change(getByLabelText('Time Zone'), {
      target: { value: 'Africa/Johannesburg' },
    });
    fireEvent.change(getByLabelText('Description'), {
      target: { value: 'test description' },
    });
  });

  expect(getByText('Create Event').form.checkValidity()).toBe(true);

  await act(async () => {
    fireEvent.click(getByText('Create Event'));
  });

  expect(eventRepository.events).toEqual([correctEvent]);
  await expect(toastPromise).resolves.toBeDefined();
  await expect(redirectionPathPromise).resolves.toBe('/');
});

test('displays error message when data could not be submitted', async () => {
  const eventRepository = new InMemoryEventRepository({ simulateBrokenConnection: true });
  const { getByLabelText, getByText, queryByText } = render(
    <EventEditor generateUuid={() => 'test uuid'} setToast={() => {}} eventRepository={eventRepository} />,
  );

  await act(async () => {
    fireEvent.change(getByLabelText('Title'), {
      target: { value: 'test title' },
    });
    fireEvent.change(getByLabelText('Date & Time'), {
      target: { value: new Date('2020-01-25T13:36') },
    });
    fireEvent.change(getByLabelText('Time Zone'), {
      target: { value: 'Africa/Johannesburg' },
    });
    fireEvent.change(getByLabelText('Description'), {
      target: { value: 'test description' },
    });
  });

  // separate act to let async event handlers catch up
  await act(async () => {
    fireEvent.click(getByText('Create Event'));
  });

  expect(queryByText('Could not create event.')).not.toBeNull();
});
