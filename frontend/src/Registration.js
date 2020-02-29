import React, { useState } from 'react';
import PropTypes from 'prop-types';
import Form from 'react-bootstrap/Form';
import Spinner from 'react-bootstrap/Spinner';
import Button from 'react-bootstrap/Button';
import Popover from 'react-bootstrap/Popover';
import OverlayTrigger from 'react-bootstrap/OverlayTrigger';
import Nav from 'react-bootstrap/Nav';
import Alert from 'react-bootstrap/Alert';
import { makeFsm, inputValueHandler } from './utils';

const FsmState = makeFsm('Displaying', 'Submitting', 'Succeeding', 'Failing');

export default function Registration({ userRepository, loggedInUser }) {
  const [fsmState, setFsmState] = useState(FsmState.Displaying);

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  function handleSubmit(event) {
    event.preventDefault();
    setFsmState(FsmState.Submitting);
    userRepository
      .register(name, email, password)
      .then(() => setFsmState(FsmState.Succeeding))
      .catch(() => setFsmState(FsmState.Failing));
  }

  function renderForm() {
    return (
      <Form onSubmit={handleSubmit}>
        {fsmState.isFailing && <Alert variant="danger">Could not perform registration.</Alert>}
        <Form.Group controlId="registration_name">
          <Form.Label>Name</Form.Label>
          <Form.Control
            name="registration_name"
            placeholder="Enter name"
            value={name}
            onChange={inputValueHandler(setName)}
            required
          />
        </Form.Group>
        <Form.Group controlId="registration_email">
          <Form.Label>Email</Form.Label>
          <Form.Control
            name="registration_email"
            placeholder="Enter email"
            type="email"
            value={email}
            onChange={inputValueHandler(setEmail)}
            required
          />
        </Form.Group>
        <Form.Group controlId="registration_password">
          <Form.Label>Password</Form.Label>
          <Form.Control
            name="registration_password"
            placeholder="Enter password"
            type="password"
            value={password}
            onChange={inputValueHandler(setPassword)}
            required
          />
        </Form.Group>
        <Button variant="primary" type="submit" style={{ width: '100%' }} disabled={fsmState.isSubmitting}>
          {fsmState.isSubmitting && (
            <Spinner
              as="span"
              animation="border"
              size="sm"
              role="status"
              aria-hidden="true"
              style={{ marginRight: '.5em' }}
            />
          )}
          Register
        </Button>
      </Form>
    );
  }

  function renderThankYou() {
    return (
      <>
        <Alert variant="success">Thank you for your registration!</Alert>
        <p>You will soon receive an email with an account activation link.</p>
      </>
    );
  }

  function renderPopover() {
    return (
      <Popover>
        <Popover.Content>{fsmState.isSucceeding ? renderThankYou() : renderForm()}</Popover.Content>
      </Popover>
    );
  }

  if (loggedInUser !== null) return <></>;
  return (
    <OverlayTrigger placement="bottom" overlay={renderPopover()} trigger="click" rootClose>
      <Nav.Link style={{ verticalAlign: 'baseline' }} variant="link">
        Register
      </Nav.Link>
    </OverlayTrigger>
  );
}

Registration.propTypes = {
  userRepository: PropTypes.shape({ register: PropTypes.func.isRequired }).isRequired,
  loggedInUser: PropTypes.shape({}),
};

Registration.defaultProps = {
  loggedInUser: null,
};
