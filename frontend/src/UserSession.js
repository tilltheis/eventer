import React, { useState, useRef } from 'react';
import PropTypes from 'prop-types';
import Popover from 'react-bootstrap/Popover';
import Form from 'react-bootstrap/Form';
import Button from 'react-bootstrap/Button';
import Overlay from 'react-bootstrap/Overlay';
import Alert from 'react-bootstrap/Alert';
import Spinner from 'react-bootstrap/Spinner';

const userSessionRepositoryPropType = PropTypes.shape({
  login: PropTypes.func.isRequired,
  logout: PropTypes.func.isRequired,
});

function Logout({ user, onLogout, userSessionRepository }) {
  const [isValid, setValid] = useState(true);
  const [isSending, setSending] = useState(false);

  function handleSubmit(event) {
    event.preventDefault();
    setSending(true);

    userSessionRepository
      .logout()
      .then(() => {
        setSending(false);
        setValid(true);
        onLogout();
      })
      .catch(() => {
        setSending(false);
        setValid(false);
      });
  }

  const [show, setShow] = useState(false);
  const target = useRef(null);

  return (
    <>
      Signed in as
      <Button style={{ verticalAlign: 'baseline' }} variant="link" ref={target} onClick={() => setShow(!show)}>
        {user.name}
      </Button>
      <Overlay target={target.current} show={show} placement="bottom">
        {props => (
          // eslint-disable-next-line react/jsx-props-no-spreading
          <Popover {...props} id="popover-basic">
            <Popover.Content>
              {isValid || <Alert variant="danger">Could not perform logout.</Alert>}
              <Form onSubmit={handleSubmit}>
                <Button variant="primary" type="submit" disabled={isSending}>
                  {isSending && (
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
        )}
      </Overlay>
    </>
  );
}

Logout.propTypes = {
  user: PropTypes.shape({ name: PropTypes.string.isRequired }).isRequired,
  onLogout: PropTypes.func.isRequired,
  userSessionRepository: userSessionRepositoryPropType.isRequired,
};

function Login({ onLogin, userSessionRepository }) {
  const [isValid, setValid] = useState(true);
  const [isSending, setSending] = useState(false);

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  function handleSubmit(event) {
    event.preventDefault();
    setSending(true);

    userSessionRepository
      .login(email, password)
      .then(() => {
        setSending(false);
        setValid(true);
        onLogin();
      })
      .catch(() => {
        setSending(false);
        setValid(false);
      });
  }

  const [show, setShow] = useState(false);
  const target = useRef(null);

  return (
    <>
      <Button style={{ verticalAlign: 'baseline' }} variant="link" ref={target} onClick={() => setShow(!show)}>
        Login
      </Button>
      <Overlay target={target.current} show={show} placement="bottom">
        {props => (
          // eslint-disable-next-line react/jsx-props-no-spreading
          <Popover {...props} id="popover-basic">
            <Popover.Content>
              {isValid || <Alert variant="danger">Invalid credentials.</Alert>}
              <Form onSubmit={handleSubmit}>
                <Form.Group controlId="email">
                  <Form.Label>Email address</Form.Label>
                  <Form.Control
                    type="email"
                    placeholder="Enter email"
                    value={email}
                    onChange={e => setEmail(e.target.value)}
                    required
                  />
                </Form.Group>
                <Form.Group controlId="password">
                  <Form.Label>Password</Form.Label>
                  <Form.Control
                    type="password"
                    placeholder="Password"
                    value={password}
                    onChange={e => setPassword(e.target.value)}
                    required
                  />
                </Form.Group>
                <Button variant="primary" type="submit" disabled={isSending}>
                  {isSending && (
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
        )}
      </Overlay>
    </>
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
