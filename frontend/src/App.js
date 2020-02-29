import React, { useState } from 'react';
import './App.css';
import Container from 'react-bootstrap/Container';
import { BrowserRouter as Router, Switch, Route, Redirect } from 'react-router-dom';
import Navbar from 'react-bootstrap/Navbar';
import Nav from 'react-bootstrap/Nav';
import Toast from './Toast';
import EventEditor from './EventEditor';
import EventList from './EventList';
import UserSession from './UserSession';
import EventRepository from './EventRepository';
import UserSessionRepository from './UserSessionRepository';
import { getCookieValue } from './utils';
import Registration from './Registration';
import UserRepository from './UserRepository';

export default function App() {
  const eventRepository = new EventRepository();
  const userSessionRepository = new UserSessionRepository();
  const userRepository = new UserRepository();
  const readLoggedInUserFromCookie = () => {
    const userCookie = getCookieValue('jwt-header.payload');
    return userCookie !== null ? JSON.parse(atob(userCookie.split('.')[1])) : null;
  };

  const [toast, setToast] = useState(undefined);
  const [loggedInUser, setLoggedInUser] = useState(readLoggedInUserFromCookie());

  return (
    <Router>
      <Container>
        <header>
          <Navbar bg="light">
            <Navbar.Brand href="/">Eventer</Navbar.Brand>
            <Nav.Link href="/events/new">Create New Event</Nav.Link>
            <Navbar.Toggle />
            <Navbar.Collapse className="justify-content-end">
              <UserSession
                userSessionRepository={userSessionRepository}
                loggedInUser={loggedInUser}
                onLoginStatusChange={() => setLoggedInUser(readLoggedInUserFromCookie())}
              />
              <Registration userRepository={userRepository} loggedInUser={loggedInUser} />
            </Navbar.Collapse>
          </Navbar>
        </header>
        <main>
          <Container style={{ position: 'relative' }}>
            {toast && <Toast header={toast.header} body={toast.body} />}
            <Switch>
              <Route exact path="/">
                <Redirect to="/events" />
              </Route>
              <Route exact path="/events">
                <EventList eventRepository={eventRepository} />
              </Route>
              <Route exact path="/events/new">
                <EventEditor setToast={setToast} eventRepository={eventRepository} />
              </Route>
              <Route>
                <Redirect to="/" />
              </Route>
            </Switch>
          </Container>
        </main>
      </Container>
    </Router>
  );
}
