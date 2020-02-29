import React from 'react';
import { render, fireEvent } from '@testing-library/react';
import { act } from 'react-dom/test-utils';
import InMemoryUserSessionRepository from './test/InMemoryUserSessionRepository';
import UserSession from './UserSession';

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
    const { getByLabelText, getAllByText, queryByText } = render(
      <UserSession
        userSessionRepository={userSessionRepository}
        getLoggedInUser={() => userSessionRepository.getLoggedInUser()}
      />,
    );

    expect(getAllByText('Login')[0]).not.toBeNull();

    expect(getAllByText('Login')[1].form.checkValidity()).toBe(false);

    await act(async () => {
      fireEvent.change(getByLabelText('Email'), {
        target: { value: correctUser.email },
      });
      fireEvent.change(getByLabelText('Password'), {
        target: { value: correctUser.password },
      });
    });

    expect(getAllByText('Login')[1].form.checkValidity()).toBe(true);

    await act(async () => {
      fireEvent.click(getAllByText('Login')[1]);
    });

    expect(queryByText(correctUser.name)).not.toBeNull();
    expect(queryByText('Logout')).not.toBeNull();
    expect(userSessionRepository.getLoggedInUser()).toEqual(correctUser);
  });

  test('displays error message when data is wrong', async () => {
    const userSessionRepository = new InMemoryUserSessionRepository([correctUser]);
    const { getByLabelText, getAllByText, queryByText, queryAllByText } = render(
      <UserSession
        userSessionRepository={userSessionRepository}
        getLoggedInUser={() => userSessionRepository.getLoggedInUser()}
      />,
    );

    await act(async () => {
      fireEvent.change(getByLabelText('Email'), {
        target: { value: correctUser.email },
      });
      fireEvent.change(getByLabelText('Password'), {
        target: { value: 'wrong password' },
      });

      fireEvent.click(getAllByText('Login')[1]);
    });

    expect(queryByText('Invalid credentials.')).not.toBeNull();
    expect(queryAllByText('Login')).toHaveLength(2);
    expect(userSessionRepository.getLoggedInUser()).toBeNull();
  });

  test('displays error message when data could not be submitted', async () => {
    const userSessionRepository = new InMemoryUserSessionRepository([correctUser], { simulateBrokenConnection: true });
    const { getByLabelText, getAllByText, queryByText, queryAllByText } = render(
      <UserSession
        userSessionRepository={userSessionRepository}
        getLoggedInUser={() => userSessionRepository.getLoggedInUser()}
      />,
    );

    await act(async () => {
      fireEvent.change(getByLabelText('Email'), {
        target: { value: correctUser.email },
      });
      fireEvent.change(getByLabelText('Password'), {
        target: { value: 'wrong password' },
      });

      fireEvent.click(getAllByText('Login')[1]);
    });

    expect(queryByText('Invalid credentials.')).not.toBeNull();
    expect(queryAllByText('Login')).toHaveLength(2);
    expect(userSessionRepository.getLoggedInUser()).toBeNull();
  });
});

describe('logout', () => {
  test('sends data to the repository and renders the login component on success', async () => {
    const userSessionRepository = new InMemoryUserSessionRepository([correctUser], {
      initialLoggedInUser: correctUser,
    });
    const { getByText, queryAllByText } = render(
      <UserSession
        userSessionRepository={userSessionRepository}
        getLoggedInUser={() => userSessionRepository.getLoggedInUser()}
      />,
    );

    expect(userSessionRepository.getLoggedInUser()).toEqual(correctUser);

    await act(async () => {
      fireEvent.click(getByText('Logout'));
    });

    expect(userSessionRepository.getLoggedInUser()).toBeNull();
    expect(queryAllByText('Login')).toHaveLength(2);
  });

  test('displays error message when data could not be submitted', async () => {
    const userSessionRepository = new InMemoryUserSessionRepository([correctUser], {
      initialLoggedInUser: correctUser,
      simulateBrokenConnection: true,
    });
    const { getByText, queryByText } = render(
      <UserSession
        userSessionRepository={userSessionRepository}
        getLoggedInUser={() => userSessionRepository.getLoggedInUser()}
      />,
    );

    expect(userSessionRepository.getLoggedInUser()).toEqual(correctUser);

    await act(async () => {
      fireEvent.click(getByText('Logout'));
    });

    expect(queryByText('Could not perform logout.')).not.toBeNull();
    expect(userSessionRepository.getLoggedInUser()).toEqual(correctUser);
    expect(queryByText('Logout')).not.toBeNull();
  });
});
