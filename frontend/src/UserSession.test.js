import React from 'react';
import { render, fireEvent } from '@testing-library/react';
import { act } from 'react-dom/test-utils';
import InMemoryUserSessionRepository from './test/InMemoryUserSessionRepository';
import UserSession from './UserSession';

const correctUser = {
  name: 'Foodibus',
  email: 'example@example.org',
  password: 's3cr3t',
};

describe('login', () => {
  test('sends data to the repository and renders the logout component on success', async () => {
    const userSessionRepository = new InMemoryUserSessionRepository([correctUser]);
    const { getByLabelText, getAllByText, getByText } = render(
      <UserSession
        userSessionRepository={userSessionRepository}
        getLoggedInUser={() => userSessionRepository.getLoggedInUser()}
      />,
    );

    await act(async () => {
      fireEvent.click(getByText('Login'));
    });

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

    expect(getByText(correctUser.name)).toBeInTheDocument();
    expect(userSessionRepository.getLoggedInUser()).toEqual(correctUser);
  });

  test('displays error message when data is wrong', async () => {
    const userSessionRepository = new InMemoryUserSessionRepository([correctUser]);
    const { getByLabelText, getAllByText, getByText } = render(
      <UserSession
        userSessionRepository={userSessionRepository}
        getLoggedInUser={() => userSessionRepository.getLoggedInUser()}
      />,
    );

    await act(async () => {
      fireEvent.click(getByText('Login'));
    });

    await act(async () => {
      fireEvent.change(getByLabelText('Email'), {
        target: { value: correctUser.email },
      });
      fireEvent.change(getByLabelText('Password'), {
        target: { value: 'wrong password' },
      });

      fireEvent.click(getAllByText('Login')[1]);
    });

    expect(getByText('Invalid credentials.')).toBeInTheDocument();
    expect(getAllByText('Login')).toHaveLength(2);
    expect(userSessionRepository.getLoggedInUser()).toBeNull();
  });

  test('displays error message when data could not be submitted', async () => {
    const userSessionRepository = new InMemoryUserSessionRepository([correctUser], { simulateBrokenConnection: true });
    const { getByLabelText, getAllByText, getByText } = render(
      <UserSession
        userSessionRepository={userSessionRepository}
        getLoggedInUser={() => userSessionRepository.getLoggedInUser()}
      />,
    );

    await act(async () => {
      fireEvent.click(getByText('Login'));
    });

    await act(async () => {
      fireEvent.change(getByLabelText('Email'), {
        target: { value: correctUser.email },
      });
      fireEvent.change(getByLabelText('Password'), {
        target: { value: 'wrong password' },
      });

      fireEvent.click(getAllByText('Login')[1]);
    });

    expect(getByText('Invalid credentials.')).toBeInTheDocument();
    expect(getAllByText('Login')).toHaveLength(2);
    expect(userSessionRepository.getLoggedInUser()).toBeNull();
  });
});

describe('logout', () => {
  test('sends data to the repository and renders the login component on success', async () => {
    const userSessionRepository = new InMemoryUserSessionRepository([correctUser], {
      initialLoggedInUser: correctUser,
    });
    const { getByText, getAllByText } = render(
      <UserSession
        userSessionRepository={userSessionRepository}
        getLoggedInUser={() => userSessionRepository.getLoggedInUser()}
      />,
    );

    await act(async () => {
      fireEvent.click(getByText(correctUser.name));
    });

    await act(async () => {
      fireEvent.click(getByText('Logout'));
    });

    expect(userSessionRepository.getLoggedInUser()).toBeNull();
    expect(getAllByText('Login')).toHaveLength(1);
  });

  test('displays error message when data could not be submitted', async () => {
    const userSessionRepository = new InMemoryUserSessionRepository([correctUser], {
      initialLoggedInUser: correctUser,
      simulateBrokenConnection: true,
    });
    const { getByText } = render(
      <UserSession
        userSessionRepository={userSessionRepository}
        getLoggedInUser={() => userSessionRepository.getLoggedInUser()}
      />,
    );

    await act(async () => {
      fireEvent.click(getByText(correctUser.name));
    });

    await act(async () => {
      fireEvent.click(getByText('Logout'));
    });

    expect(getByText('Could not perform logout.')).toBeInTheDocument();
    expect(userSessionRepository.getLoggedInUser()).toEqual(correctUser);
    expect(getByText('Logout')).toBeInTheDocument();
  });
});
