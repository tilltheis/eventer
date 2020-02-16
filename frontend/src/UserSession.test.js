import React from 'react';
import { render, fireEvent } from '@testing-library/react';
import { act } from 'react-dom/test-utils';
import InMemoryUserSessionRepository from './test/InMemoryUserSessionRepository';
import UserSession from './UserSession';
import { flushPromises } from './test/testUtils';

// this cannot be inside of `beforeEach` or within an individual test or it won't work...
jest.mock('react-bootstrap/Overlay', () => ({ children }) => children());

const correctUser = {
  name: 'Foodibus',
  email: 'example@example.org',
  password: 's3cr3t',
};

describe('login', () => {
  test('sends data to the repository and renders the logout component on success', async () => {
    const userSessionRepository = new InMemoryUserSessionRepository([correctUser]);
    const { container } = render(
      <UserSession
        userSessionRepository={userSessionRepository}
        getLoggedInUser={() => userSessionRepository.getLoggedInUser()}
      />,
    );

    expect(container.innerHTML).toMatch('Login');

    expect(container.querySelector('form').checkValidity()).toBe(false);

    fireEvent.change(container.querySelector('#email'), {
      target: { value: correctUser.email },
    });
    fireEvent.change(container.querySelector('#password'), {
      target: { value: correctUser.password },
    });

    expect(container.querySelector('form').checkValidity()).toBe(true);

    fireEvent.click(container.querySelector('button[type=submit]'));

    await act(async () => {
      await flushPromises();
      expect(container.innerHTML).toMatch(correctUser.name);
      expect(container.innerHTML).toMatch('Logout');
    });
  });

  test('displays error message when data is wrong', async () => {
    const userSessionRepository = new InMemoryUserSessionRepository([correctUser]);
    const { container } = render(
      <UserSession
        userSessionRepository={userSessionRepository}
        getLoggedInUser={() => userSessionRepository.getLoggedInUser()}
      />,
    );

    fireEvent.change(container.querySelector('#email'), {
      target: { value: correctUser.email },
    });
    fireEvent.change(container.querySelector('#password'), {
      target: { value: 'wrong password' },
    });

    fireEvent.click(container.querySelector('button[type=submit]'));

    await act(async () => {
      await flushPromises();
      expect(container.innerHTML).toMatch('danger');
      expect(userSessionRepository.getLoggedInUser()).toBeNull();
      expect(container.innerHTML).toMatch('Login');
    });
  });

  test('displays error message when data could not be submitted', async () => {
    const userSessionRepository = new InMemoryUserSessionRepository([correctUser], { simulateBrokenConnection: true });
    const { container } = render(
      <UserSession
        userSessionRepository={userSessionRepository}
        getLoggedInUser={() => userSessionRepository.getLoggedInUser()}
      />,
    );

    fireEvent.change(container.querySelector('#email'), {
      target: { value: correctUser.email },
    });
    fireEvent.change(container.querySelector('#password'), {
      target: { value: correctUser.password },
    });

    fireEvent.click(container.querySelector('button[type=submit]'));

    await act(async () => {
      await flushPromises();
      expect(container.innerHTML).toMatch('danger');
      expect(userSessionRepository.getLoggedInUser()).toBeNull();
      expect(container.innerHTML).toMatch('Login');
    });
  });
});

describe('logout', () => {
  test('sends data to the repository and renders the login component on success', async () => {
    const userSessionRepository = new InMemoryUserSessionRepository([correctUser], {
      initialLoggedInUser: correctUser,
    });
    const { container } = render(
      <UserSession
        userSessionRepository={userSessionRepository}
        getLoggedInUser={() => userSessionRepository.getLoggedInUser()}
      />,
    );

    expect(userSessionRepository.getLoggedInUser()).toEqual(correctUser);

    fireEvent.click(container.querySelector('button[type=submit]'));

    await act(async () => {
      await flushPromises();
      expect(userSessionRepository.getLoggedInUser()).toBeNull();
      expect(container.innerHTML).toMatch('Login');
    });
  });

  test('displays error message when data could not be submitted', async () => {
    const userSessionRepository = new InMemoryUserSessionRepository([correctUser], {
      initialLoggedInUser: correctUser,
      simulateBrokenConnection: true,
    });
    const { container } = render(
      <UserSession
        userSessionRepository={userSessionRepository}
        getLoggedInUser={() => userSessionRepository.getLoggedInUser()}
      />,
    );

    expect(userSessionRepository.getLoggedInUser()).toEqual(correctUser);

    fireEvent.click(container.querySelector('button[type=submit]'));

    await act(async () => {
      await flushPromises();
      expect(container.innerHTML).toMatch('danger');
      expect(userSessionRepository.getLoggedInUser()).toEqual(correctUser);
      expect(container.innerHTML).toMatch('Logout');
    });
  });
});
