import React from 'react';
import { render } from '@testing-library/react';
import InMemoryEventRepository from './test/InMemoryEventRepository';
import EventList from './EventList';
import { flushPromises } from './test/testUtils';

const correctEvent = {
  id: 'test uuid',
  hostId: '6f31ccde-4321-4cc9-9056-6c3cbd550cba',
  title: 'test title',
  dateTime: '2020-01-25T13:36+02:00[Africa/Johannesburg]',
  description: 'test description',
};

test('loads data from repository and displays it', async () => {
  const eventRepository = new InMemoryEventRepository({ initialEvents: [correctEvent] });
  const { container } = render(<EventList eventRepository={eventRepository} />);

  await flushPromises();
  expect(container.innerHTML).toMatch(correctEvent.id);
  expect(container.innerHTML).toMatch(correctEvent.title);
});

test('displays error message when data could not be loaded', async () => {
  const eventRepository = new InMemoryEventRepository({ simulateBrokenConnection: true });
  const { container } = render(<EventList eventRepository={eventRepository} />);

  await flushPromises();
  expect(container.innerHTML).not.toMatch(correctEvent.id);
  expect(container.innerHTML).toMatch('danger');
});
