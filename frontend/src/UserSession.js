import React from 'react'
import Popover from 'react-bootstrap/Popover';
import Form from 'react-bootstrap/Form';
import Button from 'react-bootstrap/Button';
import OverlayTrigger from 'react-bootstrap/OverlayTrigger';
import { Link } from 'react-router-dom';

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

function Logout() {
  return (
    <>
    Signed in as:{' '}
    <OverlayTrigger trigger="click" placement="bottom" overlay={LogoutPopover}>
      <Link to={'#'}>Mark Otto</Link>
    </OverlayTrigger>
    </>
  );
}

function LoginPopover(props) {
  return (
    <Popover {...props} id="popover-basic">
      <Popover.Content>
        <Form>
          <Form.Group controlId="formBasicEmail">
            <Form.Label>Email address</Form.Label>
            <Form.Control type="email" placeholder="Enter email" />
          </Form.Group>
          <Form.Group controlId="formBasicPassword">
            <Form.Label>Password</Form.Label>
            <Form.Control type="password" placeholder="Password" />
          </Form.Group>
          <Button variant="primary" type="submit">
            Submit
          </Button>
        </Form>
      </Popover.Content>
    </Popover>
  );
}

function Login() {
  return (
    <OverlayTrigger trigger="click" placement="bottom" overlay={LoginPopover}>
      <Link to={'#'}>Login</Link>
    </OverlayTrigger>
  );
}


export default function UserSession() {
  return Math.random() < 0.5 ? <Login /> : <Logout />;
}