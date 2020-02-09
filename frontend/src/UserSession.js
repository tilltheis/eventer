import React, { useState, useRef } from 'react';
import Popover from 'react-bootstrap/Popover';
import Form from 'react-bootstrap/Form';
import Button from 'react-bootstrap/Button';
import Overlay from 'react-bootstrap/Overlay';
import { Link } from 'react-router-dom';
import Alert from 'react-bootstrap/Alert'
import Spinner from 'react-bootstrap/Spinner';
import { getCookieValue } from './cookies';

function LogoutPopover(props) {
  return (
    <Popover {...props} id="popover-basic">
      <Popover.Content>
        <Form>
          <Button variant="primary" type="submit">
            Logout
          </Button>
        </Form>
      </Popover.Content>
    </Popover>
  );
}

function Logout({user, onLogout}) {
  const [isSending, setSending] = useState(false);

  function handleSubmit(event) {
    event.preventDefault();
    setSending(true);

    fetch(process.env.REACT_APP_API_URL + '/sessions', {
      method: 'DELETE',
      headers: {
        'X-Csrf-Token': getCookieValue('csrf-token')
      }
    }).then((response) => {
      setSending(false);

      if (response.ok) {
        onLogout();
      }
    });
  }

  const [show, setShow] = useState(false);
  const target = useRef(null);

  return (
    <>
      Signed in as{' '}
      <Link to={'#'} ref={target} onClick={() => setShow(!show)}>{user.name}</Link>
      <Overlay target={target.current} show={show} placement="bottom">
        {(props) => (
          <Popover {...props} id="popover-basic">
            <Popover.Content>
              <Form onSubmit={handleSubmit}>
                <Button variant="primary" type="submit" disabled={isSending}>
                  {isSending && <Spinner as="span" animation="border" size="sm" role="status" aria-hidden="true" style={{marginRight:'.5em'}} />}
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


function Login({onLogin}) {
  const [isValid, setValid] = useState(true);
  const [isSending, setSending] = useState(false);

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  function handleSubmit(event) {
    event.preventDefault();
    setSending(true);

    fetch(process.env.REACT_APP_API_URL + '/sessions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Csrf-Token': getCookieValue('csrf-token')
      },
      body: JSON.stringify({ email, password })
    }).then((response) => {
      setSending(false);
      setValid(response.ok);

      if (response.ok) {
        onLogin();
      }
    });
  }

  const [show, setShow] = useState(false);
  const target = useRef(null);

  return (
    <>
      <Link to={'#'} ref={target} onClick={() => setShow(!show)}>Login</Link>
      <Overlay target={target.current} show={show} placement="bottom">
        {(props) => (
          <Popover {...props} id="popover-basic">
            <Popover.Content>
              {isValid || <Alert variant={'danger'}>Invalid credentials.</Alert>}
              <Form onSubmit={handleSubmit}>
                <Form.Group controlId="formBasicEmail">
                  <Form.Label>Email address</Form.Label>
                  <Form.Control type="email" placeholder="Enter email" value={email} onChange={e => setEmail(e.target.value)} required />
                </Form.Group>
                <Form.Group controlId="formBasicPassword">
                  <Form.Label>Password</Form.Label>
                  <Form.Control type="password" placeholder="Password" value={password} onChange={e => setPassword(e.target.value)} required />
                </Form.Group>
                <Button variant="primary" type="submit" disabled={isSending}>
                  {isSending && <Spinner as="span" animation="border" size="sm" role="status" aria-hidden="true" style={{marginRight:'.5em'}} />}
                  Submit
                </Button>
              </Form>
            </Popover.Content>
          </Popover>
        )}
      </Overlay>
    </>
  );
}


export default function UserSession() {
  const userCookie = getCookieValue('jwt-header.payload');

  const [isLoggedIn, setLoggedIn] = useState(userCookie !== null);

  if (isLoggedIn) {
    const user = JSON.parse(atob(userCookie.split('.')[1]));
    return <Logout user={user} onLogout={() => setLoggedIn(false)} />;
  } else {
    return <Login onLogin={() => setLoggedIn(true)} />;
  }
}