import React from 'react';
import ReactTestUtils from "react-dom/test-utils";
import { render, fireEvent, cleanup } from '@testing-library/react';
import EventEditor from './EventEditor';

afterEach(cleanup);

// this cannot be inside of `beforeEach` or within an individual test or it won't work...
jest.mock('react-widgets/lib/DateTimePicker', () => (props) => <input id={props.id} onChange={(ev) => props.onChange(new Date(ev.target.value))} />);
// id will be set by the EventEditor to work around DropDownList impl limitations
jest.mock('react-widgets/lib/DropdownList', () => (props) => <span id={props.id + "_input"}><input value={props.defaultValue} onChange={(ev) => props.onChange({ timeZone: ev.target.value })} /></span>);

test('sends correct POST request to backend', () => {
  const { container } = render(<EventEditor generateUuid={() => 'test uuid'} />);

  expect(container.querySelector('form').checkValidity()).toBe(false);

  fireEvent.change(container.querySelector('#title'), { target: { value: 'test title' } });
  fireEvent.change(container.querySelector('#dateTime'), { target: { value: new Date('2020-01-25T13:36') } });
  fireEvent.change(container.querySelector('#timeZone'), { target: { value: 'Africa/Johannesburg' } });
  fireEvent.change(container.querySelector('#description'), { target: { value: 'test description' } });

  expect(container.querySelector('form').checkValidity()).toBe(true);

  window.fetch = jest.fn().mockImplementation((url, options) => {
    expect(url).toBe(process.env.REACT_APP_API_URL + '/events');
    expect(options.method).toBe('POST');
    const body = JSON.parse(options.body);
    expect(Object.keys(body).sort()).toEqual(['id', 'host', 'title', 'dateTime', 'description'].sort());
    expect(body.id).toBe('test uuid');
    expect(body.host).toBe('host'); // hardcoded for now
    expect(body.title).toBe('test title');
    expect(body.dateTime).toBe('2020-01-25T13:36+02:00[Africa/Johannesburg]');
    expect(body.description).toBe('test description');

    return Promise.resolve({ok: true, json: () => {}});
  });

  fireEvent.click(container.querySelector('#createEvent'));

  expect(window.fetch).toHaveBeenCalledTimes(1);
});
