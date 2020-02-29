import React from 'react';
import { render, fireEvent } from '@testing-library/react';
import { act } from 'react-dom/test-utils';
import Registration from './Registration';
import InMemoryUserRepository from './test/InMemoryUserRepository';

const correctUser = {
  name: 'Foodibus',
  email: 'example@example.org',
  password: 's3cr3t',
};

test('shows nothing if user is already logged in', async () => {
  const userRepository = new InMemoryUserRepository();
  const { container } = render(<Registration userRepository={userRepository} getLoggedInUser={() => correctUser} />);
  expect(container).toBeEmpty();
});

test('sends data to the repository and renders the logout component on success', async () => {
  const userRepository = new InMemoryUserRepository();
  const { getByLabelText, getAllByText, getByText, queryByText } = render(
    <Registration userRepository={userRepository} getLoggedInUser={() => null} />,
  );

  await act(async () => {
    fireEvent.click(getByText('Register'));
  });

  expect(getAllByText('Register')[1].form.checkValidity()).toBe(false);

  await act(async () => {
    fireEvent.change(getByLabelText('Name'), {
      target: { value: correctUser.name },
    });
    fireEvent.change(getByLabelText('Email'), {
      target: { value: correctUser.email },
    });
    fireEvent.change(getByLabelText('Password'), {
      target: { value: correctUser.password },
    });
  });

  expect(getAllByText('Register')[1].form.checkValidity()).toBe(true);

  await act(async () => {
    fireEvent.click(getAllByText('Register')[1]);
  });

  expect(queryByText('Thank you for your registration!')).not.toBeNull();
  expect(userRepository.getRegisteredUsers()).toEqual([correctUser]);
});

test('displays error message when data could not be submitted', async () => {
  const userRepository = new InMemoryUserRepository();
  userRepository.register = () => Promise.reject(new Error('not ok'));

  const { getByLabelText, getAllByText, getByText, queryByText } = render(
    <Registration userRepository={userRepository} getLoggedInUser={() => null} />,
  );

  await act(async () => {
    fireEvent.click(getByText('Register'));
  });

  await act(async () => {
    fireEvent.change(getByLabelText('Name'), {
      target: { value: correctUser.name },
    });
    fireEvent.change(getByLabelText('Email'), {
      target: { value: correctUser.email },
    });
    fireEvent.change(getByLabelText('Password'), {
      target: { value: correctUser.password },
    });
  });

  await act(async () => {
    fireEvent.click(getAllByText('Register')[1]);
  });

  expect(queryByText('Could not perform registration.')).not.toBeNull();
  expect(userRepository.getRegisteredUsers()).toEqual([]);
});
