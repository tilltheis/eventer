import React, { useState } from 'react';
import PropTypes from 'prop-types';
import Popover from 'react-bootstrap/Popover';
import Form from 'react-bootstrap/Form';
import Button from 'react-bootstrap/Button';
import Alert from 'react-bootstrap/Alert';
import Spinner from 'react-bootstrap/Spinner';
import Nav from 'react-bootstrap/Nav';
import OverlayTrigger from 'react-bootstrap/OverlayTrigger';
import { makeFsm } from './utils';

const userSessionRepositoryPropType = PropTypes.shape({
  login: PropTypes.func.isRequired,
  logout: PropTypes.func.isRequired,
});

function Logout({ user, onLogout, userSessionRepository }) {
  const FsmState = makeFsm('Displaying', 'Submitting', 'Succeeding', 'Failing');

  const [fsmState, setFsmState] = useState(FsmState.Displaying);

  function handleSubmit(event) {
    event.preventDefault();
    setFsmState(FsmState.Submitting);

    userSessionRepository
      .logout()
      .then(() => {
        setFsmState(FsmState.Succeeding);
        onLogout();
      })
      .catch(() => {
        setFsmState(FsmState.Failing);
      });
  }

  function renderPopover() {
    return (
      <Popover>
        <Popover.Content>
          {fsmState.isFailing && <Alert variant="danger">Could not perform logout.</Alert>}
          <Form onSubmit={handleSubmit}>
            <Button variant="primary" type="submit" disabled={fsmState.isSubmitting}>
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
              Logout
            </Button>
          </Form>
        </Popover.Content>
      </Popover>
    );
  }

  return (
    <>
      Logged in as
      <OverlayTrigger placement="bottom" overlay={renderPopover()} trigger="click" rootClose>
        <Button style={{ verticalAlign: 'baseline' }} variant="link">
          {user.name}
        </Button>
      </OverlayTrigger>
    </>
  );
}

Logout.propTypes = {
  user: PropTypes.shape({ name: PropTypes.string.isRequired }).isRequired,
  onLogout: PropTypes.func.isRequired,
  userSessionRepository: userSessionRepositoryPropType.isRequired,
};

function Login({ onLogin, userSessionRepository }) {
  const FsmState = makeFsm('Displaying', 'Submitting', 'Succeeding', 'Failing');

  const [fsmState, setFsmState] = useState(FsmState.Displaying);

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  function handleSubmit(event) {
    event.preventDefault();
    setFsmState(FsmState.Submitting);

    userSessionRepository
      .login(email, password)
      .then(() => {
        setFsmState(FsmState.Succeeding);
        onLogin();
      })
      .catch(() => {
        setFsmState(FsmState.Failing);
      });
  }

  function renderPopover() {
    return (
      <Popover>
        <Popover.Content>
          {fsmState.isFailing && <Alert variant="danger">Invalid credentials.</Alert>}
          <Form onSubmit={handleSubmit}>
            <Form.Group controlId="login_email">
              <Form.Label>Email</Form.Label>
              <Form.Control
                type="email"
                placeholder="Enter email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                required
              />
            </Form.Group>
            <Form.Group controlId="login_password">
              <Form.Label>Password</Form.Label>
              <Form.Control
                type="password"
                placeholder="Enter password"
                value={password}
                onChange={e => setPassword(e.target.value)}
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
              Login
            </Button>
          </Form>
        </Popover.Content>
      </Popover>
    );
  }

  return (
    <OverlayTrigger placement="bottom" overlay={renderPopover()} trigger="click" rootClose>
      <Nav.Link style={{ verticalAlign: 'baseline' }} variant="link">
        Login
      </Nav.Link>
    </OverlayTrigger>
  );
}

Login.propTypes = {
  onLogin: PropTypes.func.isRequired,
  userSessionRepository: userSessionRepositoryPropType.isRequired,
};

export default function UserSession({ userSessionRepository, getLoggedInUser }) {
  const user = getLoggedInUser();

  const [isLoggedIn, setLoggedIn] = useState(user !== null);

  if (isLoggedIn) {
    return <Logout user={user} onLogout={() => setLoggedIn(false)} userSessionRepository={userSessionRepository} />;
  }
  return <Login onLogin={() => setLoggedIn(true)} userSessionRepository={userSessionRepository} />;
}

UserSession.propTypes = {
  userSessionRepository: userSessionRepositoryPropType.isRequired,
  getLoggedInUser: PropTypes.func.isRequired,
};
