import React, { useState } from 'react';
import logo from './logo.svg';
import './App.css';
import Container from 'react-bootstrap/Container';
import EventList from './EventList';
import EventEditor from './EventEditor';
import {
  BrowserRouter as Router,
  Switch,
  Route,
  Link
} from "react-router-dom";
import uuid from 'uuid';
import Toast from './Toast';

export default function App() {
  const [toast, setToast] = useState(undefined);

  return (
    <Router>
      <Container style={{ position: 'relative' }}>
        <header>
          <h1 className="display-1"><a href="/">Eventer</a></h1>
        </header>
        <main>
          <Container>
            {toast && <Toast {...toast} />}

            <Link to="/events/new">Create New Event</Link>

            <Switch>
              <Route path="/events/new">
                <EventEditor generateUuid={uuid.v4} setToast={setToast} />
              </Route>
              <Route path="/">
                <EventList />
              </Route>
            </Switch>
          </Container>
        </main>
      </Container>
    </Router>
  );
};